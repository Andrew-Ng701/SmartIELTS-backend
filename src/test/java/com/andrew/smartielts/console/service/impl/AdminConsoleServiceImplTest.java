package com.andrew.smartielts.console.service.impl;

import com.andrew.smartielts.console.service.LearningConsoleQueryService;
import com.andrew.smartielts.dashboard.domain.vo.AdminDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.AdminUserRecordSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminConsoleServiceImplTest {

    @Mock
    private LearningConsoleQueryService learningConsoleQueryService;

    @Test
    void overviewVisual_shouldUseConsoleQueryAndBuildCharts() {
        AdminConsoleServiceImpl service = new AdminConsoleServiceImpl(learningConsoleQueryService);
        AdminModuleStatVO module = new AdminModuleStatVO();
        module.setModule("writing");
        module.setActiveCount(4L);
        module.setDeletedCount(1L);
        AdminUserRecordSummaryVO overview = new AdminUserRecordSummaryVO();
        overview.setUserId(9L);
        overview.setTotalActiveRecords(10L);
        overview.setTotalDeletedRecords(1L);

        when(learningConsoleQueryService.adminUserRecordSummary(9L)).thenReturn(overview);
        when(learningConsoleQueryService.adminModuleStats()).thenReturn(List.of(module));

        AdminDashboardOverviewVisualVO result = service.overviewVisual(1L, 9L, "last30days");

        assertNotNull(result.getSnapshotId());
        assertEquals(overview, result.getOverview());
        assertEquals(List.of(module), result.getModuleStats());
        assertEquals("bar", result.getModuleBarChart().get("chart_type"));
        assertEquals(9L, result.getAggregates().get("target_user_id"));
        verify(learningConsoleQueryService).adminUserRecordSummary(9L);
        verify(learningConsoleQueryService).adminModuleStats();
    }
}
