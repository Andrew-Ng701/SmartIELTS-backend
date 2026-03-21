package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListeningRecordMapper {

    void insert(ListeningRecord record);

    ListeningRecord findById(Long id);

    List<ListeningRecord> findByUserId(@Param("userId") Long userId);

    List<ListeningRecord> findAll();

    void updateTotalScore(@Param("id") Long id, @Param("totalScore") Integer totalScore);
}
