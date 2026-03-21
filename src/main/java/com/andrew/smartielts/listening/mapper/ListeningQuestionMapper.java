package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListeningQuestionMapper {

    void insertListeningQuestion(ListeningQuestion question);

    ListeningQuestion findById(Long id);

    List<ListeningQuestion> findByTestId(@Param("testId") Long testId);

    void updateListeningQuestion(ListeningQuestion question);

    void deleteById(Long id);

    void deleteByTestId(@Param("testId") Long testId);
}
