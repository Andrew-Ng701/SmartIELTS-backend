package com.andrew.smartielts.reading.mapper;

import com.andrew.smartielts.reading.domain.pojo.ReadingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReadingRecordMapper {

    void insert(ReadingRecord record);

    ReadingRecord findById(Long id);

    List<ReadingRecord> findByUserId(Long userId);

    List<ReadingRecord> findAll();

    void updateTotalScore(@Param("id") Long id,
                          @Param("totalScore") Integer totalScore);

    void deleteById(Long id);
}
