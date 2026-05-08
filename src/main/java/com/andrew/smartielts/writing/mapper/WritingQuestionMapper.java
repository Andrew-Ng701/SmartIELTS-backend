package com.andrew.smartielts.writing.mapper;

import com.andrew.smartielts.writing.domain.pojo.WritingQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WritingQuestionMapper {

    void insert(WritingQuestion question);

    void update(WritingQuestion question);

    List<WritingQuestion> findAll();

    WritingQuestion findById(@Param("id") Long id);

    WritingQuestion findByIdForAdmin(@Param("id") Long id);

    List<WritingQuestion> findByIdsForAdmin(@Param("ids") List<Long> ids);

    void logicalDeleteById(@Param("id") Long id,
                           @Param("deletedTime") LocalDateTime deletedTime);

    void restoreById(@Param("id") Long id);

    List<WritingQuestion> findNeedImageMigration();
}
