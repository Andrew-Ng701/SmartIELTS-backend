package com.andrew.smartielts.console.service.impl;

import com.andrew.smartielts.console.service.LearningConsoleQueryService;
import com.andrew.smartielts.dashboard.domain.vo.UserDashboardOverviewVisualVO;
import com.andrew.smartielts.dashboard.domain.vo.UserModuleStatVO;
import com.andrew.smartielts.dashboard.domain.vo.UserOverviewVO;
import com.andrew.smartielts.dashboard.domain.vo.UserProgressSummaryVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserConsoleServiceImplTest {

    @Mock
    private LearningConsoleQueryService learningConsoleQueryService;

    @Test
    void overviewVisual_shouldUseConsoleQueryPayloadAndCharts() {
        UserConsoleServiceImpl service = new UserConsoleServiceImpl(learningConsoleQueryService);
        UserOverviewVO overview = new UserOverviewVO();
        overview.setTotalActiveRecords(10L);
        overview.setTotalDeletedRecords(2L);
        UserProgressSummaryVO progress = new UserProgressSummaryVO();
        progress.setListeningAverageScore(BigDecimal.valueOf(6));
        progress.setReadingAverageScore(BigDecimal.valueOf(7));
        progress.setWritingAverageScore(BigDecimal.valueOf(5));
        progress.setSpeakingAverageScore(BigDecimal.valueOf(8));
        progress.setOverallAverageScore(BigDecimal.valueOf(6.5));
        UserModuleStatVO module = new UserModuleStatVO();
        module.setModule("reading");
        module.setActiveCount(3L);
        module.setDeletedCount(1L);

        when(learningConsoleQueryService.userOverview(9L)).thenReturn(overview);
        when(learningConsoleQueryService.userProgressSummary(9L)).thenReturn(progress);
        when(learningConsoleQueryService.userRecentRecords(9L)).thenReturn(List.of());
        when(learningConsoleQueryService.userModuleStats(9L)).thenReturn(List.of(module));

        UserDashboardOverviewVisualVO result = service.overviewVisual(9L, "last7days");

        assertNotNull(result.getSnapshotId());
        assertEquals(overview, result.getOverview());
        assertEquals(List.of(module), result.getModuleStats());
        assertEquals("radar", result.getScoreRadarChart().get("chart_type"));
        assertEquals("last7days", result.getAggregates().get("time_range"));
        verify(learningConsoleQueryService).userModuleStats(9L);
    }
}
