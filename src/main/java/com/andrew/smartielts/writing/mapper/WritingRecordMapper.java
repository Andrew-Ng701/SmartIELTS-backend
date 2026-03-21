package com.andrew.smartielts.writing.mapper;

import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WritingRecordMapper {

    void insert(WritingRecord record);

    WritingRecord findById(Long id);

    List<WritingRecord> findByUserId(Long userId);

    List<WritingRecord> findAll();

    void updateExtractedText(@Param("id") Long id,
                             @Param("extractedText") String extractedText);

    void updateAiResult(WritingRecord record);

    void updateAiStatus(@Param("id") Long id,
                        @Param("aiStatus") String aiStatus);

    void deleteById(Long id);
}
