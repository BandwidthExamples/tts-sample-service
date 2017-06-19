package com.bandwidth.tts.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.Vendor;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

public class VendorCommand extends HystrixCommand<VendorResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(VendorCommand.class);

    private final Vendor vendor;
    private final String text;
    private final VoiceNames voiceName;
    private final AudioFormats audioFormat;
    private final VendorSao vendorSao;

    protected VendorCommand(final Vendor vendor, final String text, final VoiceNames voiceName,
                            final AudioFormats audioFormat, final VendorSao vendorSao) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("Text2Speech"))
                    .andCommandKey(HystrixCommandKey.Factory.asKey(vendor.name())));
        this.vendor = vendor;
        this.text = text;
        this.voiceName = voiceName;
        this.audioFormat = audioFormat;
        this.vendorSao = vendorSao;
    }

    @Override
    protected VendorResponse getFallback() {
        LOG.warn("Returning fallback for " + vendor.name());
        // Return null so we try the next vendor in the list
        return null;
    }

    @Override
    protected VendorResponse run() {
        LOG.info("Trying vendor: " + vendor.name());
        final long vendorStartTime = System.currentTimeMillis();
        final Optional<VendorResponse> vendorResponse = vendorSao.textToSpeech(vendor, text, voiceName, audioFormat);
        LOG.info(String.format("%s took %dms", vendor.name(), System.currentTimeMillis() - vendorStartTime));

        if (vendorResponse.isPresent()) {
            if (vendorResponse.get().isSuccessful()) {
                return vendorResponse.get();
            } else {
                LOG.warn("Unsuccessful response from vendor: " + vendor.name());
            }
        } else {
            LOG.warn("No response found for vendor: " + vendor.name());
        }

        // Return null so we try the next vendor in the list
        return null;
    }
}
