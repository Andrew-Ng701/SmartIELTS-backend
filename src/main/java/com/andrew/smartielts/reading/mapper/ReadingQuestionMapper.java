package com.andrew.smartielts.reading.mapper;

import com.andrew.smartielts.reading.domain.pojo.ReadingQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingQuestionMapper {

    void insert(ReadingQuestion question);

    ReadingQuestion findById(Long id);

    List<ReadingQuestion> findByPassageId(@Param("passageId") Long passageId);

    void update(ReadingQuestion question);

    void deleteById(Long id);

    void deleteByPassageId(@Param("passageId") Long passageId);
}
