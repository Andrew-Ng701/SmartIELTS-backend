package com.andrew.smartielts.common.storage;

import com.andrew.smartielts.common.constants.StorageBizConstants;

public enum BucketType {

    WRITING_QUESTION(StorageBizConstants.BUCKET_KEY_WRITING_QUESTION),
    WRITING_RECORD(StorageBizConstants.BUCKET_KEY_WRITING_RECORD),
    LISTENING_AUDIO(StorageBizConstants.BUCKET_KEY_LISTENING_AUDIO),
    SPEAKING_AUDIO(StorageBizConstants.BUCKET_KEY_SPEAKING_AUDIO),
    QUESTION_GROUP_IMAGE(StorageBizConstants.BUCKET_KEY_QUESTION_GROUP_IMAGE);

    private final String key;

    BucketType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}