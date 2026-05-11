package com.andrew.smartielts.record.constants;

import java.util.Locale;

public final class UserRecordStateConstants {

    private UserRecordStateConstants() {}

    public static final String ACTIVE = "ACTIVE";
    public static final String DELETED = "DELETED";

    public static String normalize(String recordState) {
        if (recordState == null || recordState.isBlank()) {
            return ACTIVE;
        }
        String normalized = recordState.trim().toUpperCase(Locale.ROOT);
        if (!ACTIVE.equals(normalized) && !DELETED.equals(normalized)) {
            throw new IllegalArgumentException("Unsupported recordState: " + recordState);
        }
        return normalized;
    }
}
