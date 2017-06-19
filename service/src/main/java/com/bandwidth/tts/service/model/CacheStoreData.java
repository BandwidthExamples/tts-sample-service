package com.bandwidth.tts.service.model;

public class CacheStoreData {
    private String md5Base64;
    private long lastAccessMillis;

    public CacheStoreData(final String md5Base64, final long lastAccessMillis) {
        this.md5Base64 = md5Base64;
        this.lastAccessMillis = lastAccessMillis;
    }

    public String getMd5Base64() {
        return md5Base64;
    }

    public void setMd5Base64(final String md5Base64) {
        this.md5Base64 = md5Base64;
    }

    public long getLastAccessMillis() {
        return lastAccessMillis;
    }

    public void setLastAccessMillis(final long lastAccessMillis) {
        this.lastAccessMillis = lastAccessMillis;
    }
}
