package com.bandwidth.tts.service;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.Vendor;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;

@Service
public class FallbackSao implements VendorSao {

    private static final Logger LOG = LoggerFactory.getLogger(FallbackSao.class);

    @Override
    public Optional<VendorResponse> textToSpeech(final Vendor vendor, final String text, final VoiceNames voice, final AudioFormats audioFormat) {
        LOG.info("Using fallback vendor");
        if (audioFormat == null) {
            LOG.error("Null audio format");
            return Optional.empty();
        }

        final ClassPathResource audioFile;
        switch (audioFormat) {
            case MP3:
                audioFile = new ClassPathResource("fallback_response.mp3");
                break;
            case OGG:
                audioFile = new ClassPathResource("fallback_response.ogg");
                break;
            case PCM:
                audioFile = new ClassPathResource("fallback_response.pcm");
                break;
            default:
                LOG.error("Unknown audioFormat {}", audioFormat.name());
                return Optional.empty();
        }

        try {
            return Optional.of(new VendorResponse(audioFile.getInputStream(), false, false, true, vendor));
        } catch (final IOException e) {
            LOG.error("Unable to retrieve input stream", e);
            return Optional.empty();
        }

    }

    @Override
    public Vendor getVendor() {
        return Vendor.FALLBACK;
    }
}
