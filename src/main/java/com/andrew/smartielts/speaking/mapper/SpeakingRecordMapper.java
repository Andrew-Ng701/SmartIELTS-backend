package com.andrew.smartielts.speaking.mapper;

import com.andrew.smartielts.speaking.domain.pojo.SpeakingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpeakingRecordMapper {

    void insertSpeakingRecord(SpeakingRecord record);

    SpeakingRecord findById(Long id);

    List<SpeakingRecord> findByUserId(Long userId);

    List<SpeakingRecord> findBySessionId(String sessionId);

    SpeakingRecord findBySessionIdAndQuestionId(@Param("sessionId") String sessionId,
                                                @Param("questionId") Long questionId);

    void updateSpeakingRecord(SpeakingRecord record);
}
