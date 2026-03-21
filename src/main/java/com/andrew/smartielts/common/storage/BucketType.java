package com.andrew.smartielts.common.storage;

public enum BucketType {
    WRITING_QUESTION("writing-question"),
    WRITING_RECORD("writing-record"),
    LISTENING_RECORDING("listening-recording"),
    SPEAKING_AUDIO("speaking-audio");   // 新增

    private final String key;

    BucketType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
