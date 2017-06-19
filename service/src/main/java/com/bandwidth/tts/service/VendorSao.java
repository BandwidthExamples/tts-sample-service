package com.bandwidth.tts.service;

import java.util.Optional;

import com.bandwidth.tts.service.model.AudioFormats;
import com.bandwidth.tts.service.model.Vendor;
import com.bandwidth.tts.service.model.VendorResponse;
import com.bandwidth.tts.service.model.VoiceNames;

public interface VendorSao {
    Optional<VendorResponse> textToSpeech(Vendor vendor, String text, VoiceNames voiceName, AudioFormats audioFormat);

    Vendor getVendor();
}
