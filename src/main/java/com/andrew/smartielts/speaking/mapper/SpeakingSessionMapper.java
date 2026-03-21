package com.andrew.smartielts.speaking.mapper;

import com.andrew.smartielts.speaking.domain.pojo.SpeakingSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SpeakingSessionMapper {

    void insertSpeakingSession(SpeakingSession session);

    SpeakingSession findBySessionId(String sessionId);

    void updateSpeakingSession(SpeakingSession session);

    List<SpeakingSession> findCompletedSessionsByUserId(@Param("userId") Long userId);
}
