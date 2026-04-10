package com.andrew.smartielts.dashboard.preload.impl;

import com.andrew.smartielts.dashboard.agent.DashboardAgentContext;
import com.andrew.smartielts.dashboard.agent.DashboardCapability;
import com.andrew.smartielts.dashboard.agent.DashboardCapabilityRouter;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskObjectRef;
import com.andrew.smartielts.dashboard.controller.dto.DashboardAskPreloadedPayload;
import com.andrew.smartielts.dashboard.learning.DashboardLearningContextService;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadCacheService;
import com.andrew.smartielts.dashboard.preload.DashboardPreloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardPreloadServiceImpl implements DashboardPreloadService {

    private static final long PAGE_TTL_MILLIS = 3 * 60 * 1000L;
    private static final long DETAIL_TTL_MILLIS = 10 * 60 * 1000L;
    private static final int MAX_RECENT_QUESTION_COUNT = 10;
    private static final String PRELOAD_QUERY = "preload";
    private static final String ROLE_USER = "USER";
    private static final String PAGE_NAME_DETAIL = "detail";

    private final DashboardCapabilityRouter capabilityRouter;
    private final DashboardLearningContextService dashboardLearningContextService;
    private final DashboardPreloadCacheService dashboardPreloadCacheService;

    @Qualifier("dashboardSseExecutor")
    private final Executor dashboardSseExecutor;

    @Override
    public DashboardAskPreloadedPayload preload(
            String role,
            Long operatorUserId,
            Long targetUserId,
            String pageName,
            DashboardAskObjectRef objectRef,
            Map<String, Object> context
    ) {
        String cacheKey = buildCacheKey(role, operatorUserId, targetUserId, pageName, objectRef);
        DashboardAskPreloadedPayload cached = dashboardPreloadCacheService.get(cacheKey);
        if (cached != null) {
            return copyPayload(cached);
        }

        DashboardAskPreloadedPayload payload = buildPayload(
                role,
                operatorUserId,
                targetUserId,
                pageName,
                objectRef,
                context
        );

        dashboardPreloadCacheService.put(cacheKey, payload, chooseTtl(pageName, objectRef));
        return copyPayload(payload);
    }

    @Override
    public void preloadAsync(
            String role,
            Long operatorUserId,
            Long targetUserId,
            String pageName,
            DashboardAskObjectRef objectRef,
            Map<String, Object> context
    ) {
        dashboardSseExecutor.execute(() -> {
            try {
                preload(role, operatorUserId, targetUserId, pageName, objectRef, context);
            } catch (Exception e) {
                log.warn(
                        "Dashboard preload async failed, role={}, operatorUserId={}, targetUserId={}, pageName={}, reason={}",
                        role, operatorUserId, targetUserId, pageName, e.getMessage()
                );
            }
        });
    }

    @Override
    public DashboardAskPreloadedPayload getCached(
            String role,
            Long operatorUserId,
            Long targetUserId,
            String pageName,
            DashboardAskObjectRef objectRef
    ) {
        String cacheKey = buildCacheKey(role, operatorUserId, targetUserId, pageName, objectRef);
        DashboardAskPreloadedPayload payload = dashboardPreloadCacheService.get(cacheKey);
        return payload == null ? null : copyPayload(payload);
    }

    @Override
    public void evict(
            String role,
            Long operatorUserId,
            Long targetUserId,
            String pageName,
            DashboardAskObjectRef objectRef
    ) {
        dashboardPreloadCacheService.evict(buildCacheKey(role, operatorUserId, targetUserId, pageName, objectRef));
    }

    private DashboardAskPreloadedPayload buildPayload(
            String role,
            Long operatorUserId,
            Long targetUserId,
            String pageName,
            DashboardAskObjectRef objectRef,
            Map<String, Object> context
    ) {
        Long effectiveTargetUserId = ROLE_USER.equalsIgnoreCase(role)
                ? operatorUserId
                : (targetUserId != null ? targetUserId : operatorUserId);

        Map<String, Object> learningContext = dashboardLearningContextService.buildLearningContext(
                role,
                operatorUserId,
                effectiveTargetUserId,
                pageName,
                objectRef
        );

        DashboardAskPreloadedPayload payload = new DashboardAskPreloadedPayload();
        payload.setSnapshotId(UUID.randomUUID().toString());
        payload.setSnapshotTime(OffsetDateTime.now().toString());

        if (!isDetailOnlyPage(pageName)) {
            payload.setOverview(loadCapability(role, operatorUserId, effectiveTargetUserId, resolveOverviewCapability(role)));
            payload.setProgressSummary(loadCapability(role, operatorUserId, effectiveTargetUserId, resolveProgressCapability(role)));
            payload.setRecentRecords(castList(loadCapability(role, operatorUserId, effectiveTargetUserId, resolveRecentRecordsCapability(role))));
            payload.setModuleStats(castList(loadCapability(role, operatorUserId, effectiveTargetUserId, resolveModuleStatsCapability(role))));
        }

        payload.setRecentQuestions(extractRecentQuestions(learningContext));
        payload.setRecentPassages(extractRecentPassages(learningContext));
        payload.setAggregates(buildAggregates(payload, learningContext, context, pageName, role, effectiveTargetUserId));
        return payload;
    }

    private DashboardCapability resolveOverviewCapability(String role) {
        return ROLE_USER.equalsIgnoreCase(role)
                ? DashboardCapability.USER_SELF_OVERVIEW
                : DashboardCapability.ADMIN_USER_RECORD_SUMMARY;
    }

    private DashboardCapability resolveProgressCapability(String role) {
        return ROLE_USER.equalsIgnoreCase(role)
                ? DashboardCapability.USER_SELF_PROGRESS_SUMMARY
                : DashboardCapability.ADMIN_USER_RECORD_SUMMARY;
    }

    private DashboardCapability resolveRecentRecordsCapability(String role) {
        return ROLE_USER.equalsIgnoreCase(role)
                ? DashboardCapability.USER_SELF_RECENT_RECORDS
                : DashboardCapability.ADMIN_USER_RECORD_SUMMARY;
    }

    private DashboardCapability resolveModuleStatsCapability(String role) {
        return ROLE_USER.equalsIgnoreCase(role)
                ? DashboardCapability.USER_SELF_MODULE_STATS
                : DashboardCapability.ADMIN_MODULE_STATS;
    }

    private Object loadCapability(
            String role,
            Long operatorUserId,
            Long targetUserId,
            DashboardCapability capability
    ) {
        try {
            DashboardAgentContext agentContext = DashboardAgentContext.builder()
                    .role(role)
                    .operatorUserId(operatorUserId)
                    .targetUserId(targetUserId)
                    .originalQuery(PRELOAD_QUERY)
                    .filters(Map.of())
                    .build();
            return capabilityRouter.route(capability, agentContext);
        } catch (Exception e) {
            log.debug("Dashboard preload capability skipped, capability={}, reason={}", capability, e.getMessage());
            return null;
        }
    }

    private List<Map<String, Object>> extractRecentQuestions(Map<String, Object> learningContext) {
        if (learningContext == null) {
            return List.of();
        }
        Object questions = learningContext.get("recordQuestions");
        if (!(questions instanceof List<?> list)) {
            return List.of();
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                result.add(new LinkedHashMap<>(castMap(map)));
                if (result.size() >= MAX_RECENT_QUESTION_COUNT) {
                    break;
                }
            }
        }
        return result;
    }

    private List<Map<String, Object>> extractRecentPassages(Map<String, Object> learningContext) {
        if (learningContext == null) {
            return List.of();
        }
        Object passage = learningContext.get("passage");
        if (passage instanceof Map<?, ?> map && !map.isEmpty()) {
            return List.of(new LinkedHashMap<>(castMap(map)));
        }
        return List.of();
    }

    private Map<String, Object> buildAggregates(
            DashboardAskPreloadedPayload payload,
            Map<String, Object> learningContext,
            Map<String, Object> context,
            String pageName,
            String role,
            Long targetUserId
    ) {
        Map<String, Object> aggregates = new LinkedHashMap<>();
        aggregates.put("hasOverview", payload.getOverview() != null);
        aggregates.put("hasProgressSummary", payload.getProgressSummary() != null);
        aggregates.put("recentRecordCount", payload.getRecentRecords() == null ? 0 : payload.getRecentRecords().size());
        aggregates.put("moduleStatCount", payload.getModuleStats() == null ? 0 : payload.getModuleStats().size());
        aggregates.put("recentQuestionCount", payload.getRecentQuestions() == null ? 0 : payload.getRecentQuestions().size());
        aggregates.put("recentPassageCount", payload.getRecentPassages() == null ? 0 : payload.getRecentPassages().size());
        aggregates.put("hasLearningContext", learningContext != null && !learningContext.isEmpty());
        aggregates.put("pageName", safeString(pageName));
        aggregates.put("role", safeString(role));
        aggregates.put("targetUserId", targetUserId);
        if (context != null && !context.isEmpty()) {
            aggregates.put("requestContext", new LinkedHashMap<>(context));
        }
        return aggregates;
    }

    private long chooseTtl(String pageName, DashboardAskObjectRef objectRef) {
        return isDetailOnlyPage(pageName) || objectRef != null
                ? DETAIL_TTL_MILLIS
                : PAGE_TTL_MILLIS;
    }

    private boolean isDetailOnlyPage(String pageName) {
        return safeString(pageName).toLowerCase().contains(PAGE_NAME_DETAIL);
    }

    private String buildCacheKey(
            String role,
            Long operatorUserId,
            Long targetUserId,
            String pageName,
            DashboardAskObjectRef objectRef
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(safeString(role)).append(':')
                .append(operatorUserId).append(':')
                .append(targetUserId).append(':')
                .append(safeString(pageName));

        if (objectRef != null) {
            sb.append(':').append(safeString(objectRef.getModule()))
                    .append(':').append(safeString(objectRef.getObjectType()))
                    .append(':').append(objectRef.getTestId())
                    .append(':').append(objectRef.getPassageId())
                    .append(':').append(objectRef.getQuestionId())
                    .append(':').append(objectRef.getRecordId())
                    .append(':').append(objectRef.getQuestionNumber())
                    .append(':').append(safeString(objectRef.getSessionId()));
        }
        return sb.toString();
    }

    private DashboardAskPreloadedPayload copyPayload(DashboardAskPreloadedPayload source) {
        if (source == null) {
            return null;
        }
        DashboardAskPreloadedPayload target = new DashboardAskPreloadedPayload();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    @SuppressWarnings("unchecked")
    private List<?> castList(Object value) {
        return value instanceof List<?> list ? list : List.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}