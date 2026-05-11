package com.andrew.smartielts.record.constants;

import java.util.Locale;
import java.util.Set;

public final class UserRecordModuleConstants {

    private UserRecordModuleConstants() {}

    public static final String READING = "READING";
    public static final String LISTENING = "LISTENING";
    public static final String WRITING = "WRITING";
    public static final String SPEAKING = "SPEAKING";

    private static final Set<String> SUPPORTED_MODULE_TYPES = Set.of(
            READING,
            LISTENING,
            WRITING,
            SPEAKING
    );

    public static String normalize(String moduleType) {
        if (moduleType == null || moduleType.isBlank()) {
            throw new IllegalArgumentException("moduleType is required");
        }
        String normalized = moduleType.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_MODULE_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported moduleType: " + moduleType);
        }
        return normalized;
    }
}
