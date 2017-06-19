package com.bandwidth.tts.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.amazonaws.services.polly.AmazonPolly;
import com.amazonaws.services.polly.AmazonPollyClientBuilder;
import com.amazonaws.services.polly.model.SynthesizeSpeechRequest;
import com.amazonaws.services.polly.model.SynthesizeSpeechResult;
import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.Vendor;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;

@Service
class PollySao implements VendorSao {
    private static final int SAMPLE_RATE = 16000;
    private static final Logger LOG = LoggerFactory.getLogger(PollySao.class);
    private AmazonPolly client;

    @Autowired
    public PollySao() {
        client = AmazonPollyClientBuilder.defaultClient();
    }

    @Override
    public Optional<VendorResponse> textToSpeech(final Vendor vendor, final String text, final VoiceNames voice, final AudioFormats audioFormat) {
        final SynthesizeSpeechRequest request = new SynthesizeSpeechRequest();
        request.setOutputFormat(audioFormat.getAwsOutputFormat());
        request.setText(text);
        request.setSampleRate(String.valueOf(SAMPLE_RATE));
        request.setVoiceId(voice.getAwsVoiceId());

        try {
            final SynthesizeSpeechResult synthesizeSpeechResult = client.synthesizeSpeech(request);
            return Optional.of(new VendorResponse(synthesizeSpeechResult.getAudioStream(), true, false, true, Vendor.POLLY));
        } catch (final Exception e) {
            LOG.error(e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Vendor getVendor() {
        return Vendor.POLLY;
    }
}
