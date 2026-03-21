package com.andrew.smartielts.writing.mapper;

import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WritingQuestionMapper {
    void insertWritingQuestion(WritingQuestion question);
    WritingQuestion findById(Long id);
    List<WritingQuestion> findAll();
    void updateWritingQuestion(WritingQuestion question);
    void deleteById(Long id);
}
