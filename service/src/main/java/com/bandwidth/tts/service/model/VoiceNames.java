package com.bandwidth.tts.service.model;

import org.apache.commons.lang3.EnumUtils;

import com.amazonaws.services.polly.model.VoiceId;

/*
 A faux enum meant to be populated with values for vendors.
 Should be able to map voice names to either Polly or another vendor.
 */
public class VoiceNames {

    private final VoiceId awsVoiceId;
    private final String altVendorId;

    VoiceNames(final VoiceId awsVoiceId, final String altVendorId) {
        this.awsVoiceId = awsVoiceId;
        this.altVendorId = altVendorId;
    }

    public static VoiceNames valueOf(final String value) {
        return new VoiceNames(EnumUtils.getEnum(VoiceId.class, value), "");
    }

    public VoiceId getAwsVoiceId() {
        return awsVoiceId;
    }

    public String getAltVendorId() {
        return altVendorId;
    }

    public String name() {
        return awsVoiceId != null ? awsVoiceId.name() : null;
    }

}
