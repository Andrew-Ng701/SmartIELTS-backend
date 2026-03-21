package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningAnswerRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListeningAnswerRecordMapper {

    void insert(ListeningAnswerRecord record);

    List<ListeningAnswerRecord> findByRecordId(@Param("recordId") Long recordId);
}
