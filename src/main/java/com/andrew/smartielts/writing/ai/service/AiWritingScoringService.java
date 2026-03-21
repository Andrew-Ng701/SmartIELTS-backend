package com.andrew.smartielts.writing.ai.service;

import com.andrew.smartielts.writing.ai.AiWritingScore;
import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import com.andrew.smartielts.writing.domain.pojo.WritingRecord;

public interface AiWritingScoringService {
    AiWritingScore score(WritingQuestion question, WritingRecord record, String finalText);
}
