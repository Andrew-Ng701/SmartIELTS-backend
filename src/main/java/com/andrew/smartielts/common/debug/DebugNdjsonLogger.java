package com.andrew.smartielts.common.debug;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

public class DebugNdjsonLogger {
    private static final String SESSION_ID = "d6dbe9";
    private static final Path LOG_PATH = Path.of("debug-d6dbe9.log");

    public static void log(String runId, String hypothesisId, String location, String message, Map<String, Object> data) {
        long ts = System.currentTimeMillis();
        String json = toJsonLine(runId, hypothesisId, location, message, data, ts);
        try {
            Files.writeString(
                    LOG_PATH,
                    json + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (Exception ignored) {
        }
    }

    private static String toJsonLine(String runId, String hypothesisId, String location, String message, Map<String, Object> data, long timestamp) {
        StringBuilder sb = new StringBuilder(256);
        sb.append("{");
        sb.append("\"sessionId\":").append(q(SESSION_ID)).append(",");
        sb.append("\"runId\":").append(q(runId)).append(",");
        sb.append("\"hypothesisId\":").append(q(hypothesisId)).append(",");
        sb.append("\"location\":").append(q(location)).append(",");
        sb.append("\"message\":").append(q(message)).append(",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"data\":").append(mapToJson(data));
        sb.append("}");
        return sb.toString();
    }

    private static String mapToJson(Map<String, Object> m) {
        if (m == null || m.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append(q(e.getKey())).append(":").append(val(e.getValue()));
        }
        sb.append("}");
        return sb.toString();
    }

    private static String val(Object v) {
        if (v == null) return "null";
        if (v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        return q(String.valueOf(v));
    }

    private static String q(String s) {
        if (s == null) return "null";
        return "\"" + s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "") + "\"";
    }
}

