package com.andrew.smartielts.writing.mapper;

import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface WritingRecordAttachmentMapper {

    void insert(WritingRecordAttachment attachment);

    List<WritingRecordAttachment> findByRecordId(Long recordId);

    List<Map<String, Object>> countByRecordIds(@Param("recordIds") List<Long> recordIds);

    void updateOcrText(WritingRecordAttachment attachment);

    void deleteByRecordId(Long recordId);
}
