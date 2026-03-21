package com.andrew.smartielts.speaking.service.user.impl;

import com.andrew.smartielts.speaking.ai.SpeakingScoreAiProperties;
import com.andrew.smartielts.speaking.ai.dto.SpeakingFinalEvaluationResult;
import com.andrew.smartielts.speaking.ai.service.impl.SpeakingFinalEvaluationServiceImpl;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpeakingFinalEvaluationServiceImplTest {

    private RestTemplate restTemplate;
    private SpeakingFinalEvaluationServiceImpl service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);

        SpeakingScoreAiProperties props = new SpeakingScoreAiProperties();
        props.setBaseUrl("https://mock-ai.example.com");
        props.setApiKey("test-key");
        props.setModel("qwen-plus");

        service = new SpeakingFinalEvaluationServiceImpl(props, restTemplate);
    }

    @Test
    void evaluateFinal_shouldParseJsonResponse() {
        SpeakingQuestion question = new SpeakingQuestion();
        question.setId(101L);
        question.setPart("PART1");
        question.setQuestionText("Do you enjoy reading?");

        SpeakingRecord record = new SpeakingRecord();
        record.setQuestionId(101L);
        record.setTranscript("Yes, I enjoy reading in my free time.");
        record.setFluencyAndCoherence(new BigDecimal("6.5"));
        record.setLexicalResource(new BigDecimal("6.0"));
        record.setGrammaticalRangeAndAccuracy(new BigDecimal("6.0"));
        record.setPronunciation(new BigDecimal("6.5"));
        record.setOverallScore(new BigDecimal("6.5"));
        record.setFeedback("Clear enough.");

        String aiJson = """
                {
                  "fluencyAndCoherence": 6.5,
                  "lexicalResource": 6.5,
                  "grammaticalRangeAndAccuracy": 6.0,
                  "pronunciation": 6.5,
                  "overallScore": 6.5,
                  "feedback": "The candidate shows a reasonably effective overall speaking performance with fairly stable fluency and pronunciation."
                }
                """;

        Map<String, Object> responseBody = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("content", aiJson))
                )
        );

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(responseBody, HttpStatus.OK));

        SpeakingFinalEvaluationResult result = service.evaluateFinal(
                "sess-000001",
                Map.of(101L, question),
                List.of(record),
                new BigDecimal("6.5"),
                new BigDecimal("6.5"),
                new BigDecimal("6.0"),
                new BigDecimal("6.5"),
                new BigDecimal("6.4")
        );

        assertEquals(new BigDecimal("6.5"), result.getFluencyAndCoherence());
        assertEquals(new BigDecimal("6.5"), result.getLexicalResource());
        assertEquals(new BigDecimal("6.0"), result.getGrammaticalRangeAndAccuracy());
        assertEquals(new BigDecimal("6.5"), result.getPronunciation());
        assertEquals(new BigDecimal("6.5"), result.getOverallScore());
        assertTrue(result.getFeedback().contains("reasonably effective"));
        assertNotNull(result.getRawContent());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(
                urlCaptor.capture(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        );

        assertEquals("https://mock-ai.example.com/chat/completions", urlCaptor.getValue());
    }

    @Test
    void evaluateFinal_whenNoChoices_shouldThrowException() {
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(Map.class)
        )).thenReturn(new ResponseEntity<>(Map.of(), HttpStatus.OK));

        SpeakingQuestion question = new SpeakingQuestion();
        question.setId(101L);

        SpeakingRecord record = new SpeakingRecord();
        record.setQuestionId(101L);
        record.setTranscript("test");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.evaluateFinal(
                        "sess-000001",
                        Map.of(101L, question),
                        List.of(record),
                        new BigDecimal("6.5"),
                        new BigDecimal("6.5"),
                        new BigDecimal("6.0"),
                        new BigDecimal("6.5"),
                        new BigDecimal("6.4")
                )
        );

        assertTrue(ex.getMessage().contains("Failed to evaluate final speaking session"));
    }
}
