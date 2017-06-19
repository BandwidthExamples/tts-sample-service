package com.bandwidth.tts.service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.Vendor;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;

@Service
public class CacheService {
    private String s3Bucket;
    private String cachePrefix;
    private AmazonS3 s3Client;

    private static final Logger LOG = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    public CacheService(@Value("${tts.cache.s3.bucket}") final String s3Bucket,
                        @Value("${tts.cache.s3.prefix}") final String cachePrefix) {
        this.s3Bucket = s3Bucket;
        this.cachePrefix = cachePrefix;
        this.s3Client = AmazonS3ClientBuilder.defaultClient();
    }

    public Optional<VendorResponse> getCachedResponse(final String text, final VoiceNames voice, final AudioFormats audioFormat) {
        final long startTime = System.currentTimeMillis();
        final String keyString = createKeyString(text, voice, audioFormat);
        LOG.info("Looking for cached object " + keyString);
        if (s3Client.doesObjectExist(s3Bucket, keyString)) {
            LOG.info("Found S3 object " + keyString);
            final S3Object cacheObject = s3Client.getObject(s3Bucket, keyString);
            final InputStream s3Stream = cacheObject.getObjectContent();
            try {
                LOG.info("Reading bytes");
                final byte[] s3Bytes = IOUtils.toByteArray(s3Stream);
                LOG.info(String.format("Loaded cached response in: %dms", System.currentTimeMillis() - startTime));
                return Optional.of(new VendorResponse(new ByteArrayInputStream(s3Bytes), false, true, true, Vendor.CACHE));
            } catch (final Exception e) {
                return Optional.empty();
            } finally {
                IOUtils.closeQuietly(s3Stream);
            }
        } else {
            LOG.info("No response found for " + keyString);
            return Optional.empty();
        }
    }

    public void putCacheResponse(final String text, final VoiceNames voice, final AudioFormats audioFormat, final VendorResponse vendorResponse) {
        LOG.info(String.format("Attempting to cache entry for [%s] from vendor %s", StringUtils.truncate(text, 100), vendorResponse.getVendor().toString()));
        final String keyString = createKeyString(text, voice, audioFormat);
        final byte[] responseBytes = vendorResponse.getOutputStream().toByteArray();
        final byte[] responseMD5 = getMD5DigestForBytes(responseBytes);
        final String responseMD5Base64 = DatatypeConverter.printBase64Binary(responseMD5);

        try {
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(responseBytes.length);
            final PutObjectResult result = s3Client.putObject(s3Bucket, keyString, new ByteArrayInputStream(responseBytes), metadata);
            if (result.getContentMd5().equals(responseMD5Base64)) {
                LOG.info(String.format("Entry for [%s] from vendor %s successfully added to S3 with key %s", StringUtils.truncate(text, 100), vendorResponse.getVendor().name(), keyString));
            } else {
                LOG.info(String.format("MD5 sum mismatch. Expected [%s] but received [%s]", responseMD5Base64, result.getContentMd5()));
                s3Client.deleteObject(s3Bucket, keyString);
            }
        } catch (Exception e) {
            LOG.error("Error occurred while trying to cache an utterance.", e);
            try {
                // Send a delete so a corrupt instance doesn't remain in S3.
                s3Client.deleteObject(s3Bucket, keyString);
            } catch (AmazonClientException e2) {
                // Don't want to duplicate logs of non-transient S3 issues.
            }
            throw e;
        }
    }

    private String createKeyString(final String text, final VoiceNames voice, final AudioFormats audioFormat) {
        // prefix/voice/md5.output_format
        return String.format("%s/%s/%s.%s", cachePrefix, voice.name(),
            DatatypeConverter.printBase64Binary(getMD5DigestForBytes(text.toLowerCase().getBytes())),
            audioFormat.getAwsOutputFormat().toString());
    }

    private byte[] getMD5DigestForBytes(byte[] bytes) {
        try {
            return MessageDigest.getInstance("MD5").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            LOG.error("MD5 algorithm not found.");
        }
        return new byte[0];
    }
}
