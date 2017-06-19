package com.bandwidth.tts.service.model;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.TeeInputStream;

public class VendorResponse implements AutoCloseable {
    private final InputStream inputStream;
    private final ByteArrayOutputStream outputStream;
    private final Boolean isCachable;
    private final Boolean isCachedResponse;
    private Boolean isSuccessful;
    private final Vendor vendor;
    private long streamEndTime;

    public VendorResponse(final InputStream inputStream, final Boolean isCachable, final Boolean isCachedResponse, final Boolean isSuccessful, final Vendor vendor) {
        this.outputStream = new ByteArrayOutputStream();
        if (inputStream != null) {
            this.inputStream = new TeeInputStream(inputStream, outputStream, true);
        } else {
            this.inputStream = null;
        }
        this.isCachable = isCachable;
        this.isCachedResponse = isCachedResponse;
        this.isSuccessful = isSuccessful;
        this.vendor = vendor;
    }

    public ByteArrayOutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Boolean isCachable() {
        return isCachable;
    }

    public Boolean isCachedResponse() {
        return isCachedResponse;
    }

    public Boolean isSuccessful() {
        return isSuccessful;
    }

    public void setSuccessful(final Boolean successful) {
        isSuccessful = successful;
    }

    public Vendor getVendor() {
        return vendor;
    }

    public long getStreamEndTime() {
        return streamEndTime;
    }

    public void setStreamEndTime(final long streamEndTime) {
        this.streamEndTime = streamEndTime;
    }

    @Override
    public void close() throws Exception {
        IOUtils.closeQuietly(inputStream);
    }
}
