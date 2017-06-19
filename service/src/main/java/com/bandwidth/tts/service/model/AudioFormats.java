package com.bandwidth.tts.service.model;

import com.amazonaws.services.polly.model.OutputFormat;

public enum AudioFormats {
    PCM("audio/pcm", OutputFormat.Pcm),
    MP3("audio/mpeg", OutputFormat.Mp3),
    OGG("audio/ogg", OutputFormat.Ogg_vorbis);

    private final String mimeType;
    private final OutputFormat awsOutputFormat;

    AudioFormats(final String mimeType, final OutputFormat awsOutputFormat) {
        this.mimeType = mimeType;
        this.awsOutputFormat = awsOutputFormat;
    }

    public String getMimeType() {
        return mimeType;
    }

    public OutputFormat getAwsOutputFormat() {
        return awsOutputFormat;
    }
}
