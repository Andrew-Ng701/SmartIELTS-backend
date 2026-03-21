package com.andrew.smartielts.reading.mapper;

import com.andrew.smartielts.reading.domain.pojo.ReadingAnswerRecord;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReadingAnswerRecordMapper {

    void insert(ReadingAnswerRecord record);

    List<ReadingAnswerRecord> findByRecordId(Long recordId);

    void deleteByRecordId(Long recordId);
}
