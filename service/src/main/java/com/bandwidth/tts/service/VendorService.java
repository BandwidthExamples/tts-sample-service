package com.bandwidth.tts.service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;

import org.apache.commons.configuration.AbstractConfiguration;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.Vendor;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;
import com.netflix.config.ConfigurationManager;

@Service
public class VendorService implements ApplicationContextAware {
    private final CacheService cacheService;
    private final ExecutorService backgroundExecutorService;
    private ApplicationContext applicationContext;
    private List<Vendor> vendors;
    private static final Logger LOG = LoggerFactory.getLogger(VendorService.class);

    @Autowired
    public VendorService(final CacheService cacheService) {
        this.cacheService = cacheService;
        this.backgroundExecutorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.vendors = new LinkedList<>();
        vendors.add(Vendor.POLLY);
        vendors.add(Vendor.FALLBACK);
    }

    @PostConstruct
    public void init() {
        final AbstractConfiguration configInstance = ConfigurationManager.getConfigInstance();

        vendors.forEach(vendor -> {
            configInstance.setProperty("hystrix.command." + vendor.name() + ".circuitBreaker.enabled", "true");
            configInstance.setProperty("hystrix.command." + vendor.name() + ".circuitBreaker.requestVolumeThreshold", "10");
            configInstance.setProperty("hystrix.command." + vendor.name() + ".metrics.rollingStats.timeInMilliseconds", "60000");

            // Timeout requests after 2 seconds
            configInstance.setProperty("hystrix.command." + vendor.name() + ".execution.isolation.thread.timeoutInMilliseconds", "2000");

            // Don't attempt to re-close the circuit for at least 30 seconds
            configInstance.setProperty("hystrix.command." + vendor.name() + ".circuitBreaker.sleepWindowInMilliseconds", "30000");
        });
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public VendorResponse textToSpeech(final String text, final VoiceNames voiceName, final AudioFormats audioFormat, final OutputStream outputStream) {
        final long start = System.currentTimeMillis();
        try {
            LOG.info(String.format("Processing text to speech request for: [%s]", text));
            final String filteredText = CharsetUtil.filterUnsupportedCharacters(text);

            if (text.length() != filteredText.length()) {
                LOG.info(String.format("Filtered requested text to: [%s]", filteredText));
            }

            Optional<VendorResponse> cacheResponseOptional = cacheService.getCachedResponse(filteredText, voiceName, audioFormat);
            if (cacheResponseOptional.isPresent()) {
                VendorResponse response = cacheResponseOptional.get();
                return writeOutputStream(response, outputStream);
            }

            return vendors.stream()
                          .map(vendor -> createVendorCommand(vendor, filteredText, voiceName, audioFormat))
                          .filter(vendorCommand -> vendorCommand != null)
                          .map(VendorCommand::execute)
                          .filter(vendorResponse -> vendorResponse != null && vendorResponse.getInputStream() != null)
                          .map(vendorResponse -> writeOutputStream(vendorResponse, outputStream))
                          .filter(vendorResponse -> vendorResponse != null)
                          .map(vendorResponse -> asyncCacheVendorResponse(filteredText, voiceName, audioFormat, vendorResponse))
                          .findFirst()
                          .orElse(null);

        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            final long duration = System.currentTimeMillis() - start;
            LOG.info(String.format("Finished processing text to speech request for: [%s] in %dms", text, duration));
        }
    }

    private Optional<VendorSao> getVendorSaoByName(final String vendorName) {
        final Map<String, VendorSao> vendorSaos = applicationContext.getBeansOfType(VendorSao.class);
        return vendorSaos.values()
                         .stream()
                         .filter(vendorSao -> vendorSao.getVendor().name().equalsIgnoreCase(vendorName))
                         .findFirst();
    }

    private VendorCommand createVendorCommand(final Vendor vendor, final String text, final VoiceNames voiceName, final AudioFormats audioFormat) {
        final Optional<VendorSao> optionalVendorSao = getVendorSaoByName(vendor.name());
        if (optionalVendorSao.isPresent()) {
            return new VendorCommand(vendor, text, voiceName, audioFormat, optionalVendorSao.get());
        } else {
            LOG.warn("No vendor sao found for vendor: " + vendor.name());
            return null;
        }
    }

    // Manually write the input stream to the output stream as soon as we start getting bytes
    private VendorResponse writeOutputStream(final VendorResponse vendorResponse, final OutputStream outputStream) {
        try (InputStream vendorInputStream = vendorResponse.getInputStream(); BufferedInputStream bufferedInputStream = new BufferedInputStream(vendorInputStream)) {
            boolean failedOutput = false;
            byte[] buffer = new byte[1024];
            int len;
            LOG.debug("Ready to write to output stream");
            while ((len = bufferedInputStream.read(buffer)) != -1) {
                // Surround in a separate try/catch to make sure we can read the entire input stream.
                if (!failedOutput) {
                    try {
                        outputStream.write(buffer, 0, len);
                        outputStream.flush();
                    } catch (IOException io) {
                        LOG.error("Failed to write to output stream due to exception:\n" + io.getMessage());
                        failedOutput = true;
                    }
                }
            }
            LOG.debug("Output stream has been completely written");
            vendorResponse.setStreamEndTime(System.currentTimeMillis());
            vendorResponse.setSuccessful(true);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            vendorResponse.setSuccessful(false);
        } finally {
            IOUtils.closeQuietly(outputStream);
        }

        return vendorResponse;
    }

    private VendorResponse asyncCacheVendorResponse(final String text, final VoiceNames voice, final AudioFormats audioFormat, final VendorResponse vendorResponse) {
        if (vendorResponse.isCachable() && vendorResponse.isSuccessful()) {
            backgroundExecutorService.submit(() -> cacheService.putCacheResponse(text, voice, audioFormat, vendorResponse));
        }
        return vendorResponse;
    }
}
