package com.andrew.smartielts.dashboard.support;

import java.util.LinkedHashMap;
import java.util.Map;

public class DashboardTraceTimer {

    private final String traceId;
    private final long startedAt;
    private final Map<String, Long> marks = new LinkedHashMap<>();

    public DashboardTraceTimer(String traceId) {
        this.traceId = traceId;
        this.startedAt = System.currentTimeMillis();
    }

    public String getTraceId() {
        return traceId;
    }

    public long totalElapsedMs() {
        return System.currentTimeMillis() - startedAt;
    }

    public long mark(String stage) {
        long now = System.currentTimeMillis();
        long elapsed = now - startedAt;
        marks.put(stage, elapsed);
        return elapsed;
    }

    public Map<String, Long> snapshot() {
        return new LinkedHashMap<>(marks);
    }
}