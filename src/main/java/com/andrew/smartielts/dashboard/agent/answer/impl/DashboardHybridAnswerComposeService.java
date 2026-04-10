package com.andrew.smartielts.dashboard.agent.answer.impl;

import com.andrew.smartielts.dashboard.agent.answer.DashboardAnswerComposeService;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerComposeResult;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerRewriteRequest;
import com.andrew.smartielts.dashboard.agent.answer.dto.DashboardAnswerRewriteResult;
import com.andrew.smartielts.dashboard.agent.answer.llm.DashboardAnswerRewriteLlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class DashboardHybridAnswerComposeService implements DashboardAnswerComposeService {

    private final DashboardTemplateAnswerComposeService templateAnswerComposeService;
    private final DashboardAnswerRewriteLlmClient rewriteLlmClient;

    @Override
    public DashboardAnswerComposeResult compose(DashboardAnswerComposeRequest request) {
        DashboardAnswerComposeResult templateResult = templateAnswerComposeService.compose(request);

        try {
            DashboardAnswerRewriteResult rewriteResult = rewriteLlmClient.rewrite(
                    DashboardAnswerRewriteRequest.builder()
                            .role(request.getRole())
                            .originalQuery(request.getOriginalQuery())
                            .capability(request.getCapability())
                            .filters(request.getFilters())
                            .data(request.getData())
                            .factualSummary(templateResult.getAnswer())
                            .suggestions(templateResult.getSuggestions())
                            .responseLanguage(request.getResponseLanguage())
                            .build()
            );

            return DashboardAnswerComposeResult.builder()
                    .answer(rewriteResult.getAnswer())
                    .suggestions(rewriteResult.getSuggestions() == null || rewriteResult.getSuggestions().isEmpty()
                            ? templateResult.getSuggestions()
                            : rewriteResult.getSuggestions())
                    .build();
        } catch (Exception e) {
            log.warn("AI answer rewrite failed, fallback to template answer {}", e.getMessage());
            return templateResult;
        }
    }
}