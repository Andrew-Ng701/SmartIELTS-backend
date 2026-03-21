package com.andrew.smartielts.speaking.service.admin.impl;

import com.andrew.smartielts.speaking.domain.pojo.SpeakingQuestion;
import com.andrew.smartielts.speaking.mapper.SpeakingMapper;
import com.andrew.smartielts.speaking.service.admin.AdminSpeakingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminSpeakingServiceImpl implements AdminSpeakingService {

    @Autowired
    private SpeakingMapper speakingMapper;

    @Override
    public SpeakingQuestion createSpeakingQuestion(SpeakingQuestion question) {
        question.setId(null);
        question.setCreatedTime(LocalDateTime.now());
        if (question.getActive() == null) {
            question.setActive(1);
        }
        if (question.getDisplayOrder() == null) {
            question.setDisplayOrder(0);
        }
        speakingMapper.insertSpeakingQuestion(question);
        return question;
    }

    @Override
    public List<SpeakingQuestion> listAllSpeakingQuestion() {
        return speakingMapper.findAll();
    }

    @Override
    public SpeakingQuestion getSpeakingQuestion(Long id) {
        SpeakingQuestion question = speakingMapper.findById(id);
        if (question == null) {
            throw new RuntimeException("Speaking question not found");
        }
        return question;
    }

    @Override
    public SpeakingQuestion updateSpeakingQuestion(Long id, SpeakingQuestion question) {
        SpeakingQuestion existing = speakingMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Speaking question not found");
        }
        existing.setPart(question.getPart());
        existing.setSubType(question.getSubType());
        existing.setTopicKey(question.getTopicKey());
        existing.setQuestionText(question.getQuestionText());
        existing.setCueCard(question.getCueCard());
        existing.setFollowUpQuestionsJson(question.getFollowUpQuestionsJson());
        existing.setPrepSeconds(question.getPrepSeconds());
        existing.setAnswerSeconds(question.getAnswerSeconds());
        existing.setDisplayOrder(question.getDisplayOrder());
        existing.setActive(question.getActive());
        speakingMapper.updateSpeakingQuestion(existing);
        return existing;
    }

    @Override
    public void deleteSpeakingQuestionById(Long id) {
        SpeakingQuestion existing = speakingMapper.findById(id);
        if (existing == null) {
            throw new RuntimeException("Speaking question not found");
        }
        speakingMapper.deleteById(id);
    }
}
