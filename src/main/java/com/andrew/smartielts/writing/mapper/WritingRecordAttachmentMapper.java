package com.andrew.smartielts.writing.mapper;

import com.andrew.smartielts.writing.domain.pojo.WritingRecordAttachment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface WritingRecordAttachmentMapper {

    void insert(WritingRecordAttachment attachment);

    List<WritingRecordAttachment> findByRecordId(Long recordId);

    void updateOcrText(WritingRecordAttachment attachment);

    void deleteByRecordId(Long recordId);
}
