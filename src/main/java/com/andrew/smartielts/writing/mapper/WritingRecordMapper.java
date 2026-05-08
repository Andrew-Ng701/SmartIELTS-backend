package com.andrew.smartielts.writing.mapper;

import com.andrew.smartielts.writing.domain.pojo.WritingRecord;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.admin.AdminWritingRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingDeletedRecordPageQuery;
import com.andrew.smartielts.writing.domain.query.user.UserWritingRecordPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface WritingRecordMapper {

    void insert(WritingRecord record);

    WritingRecord findAnyById(@Param("id") Long id);

    WritingRecord findByIdForAdmin(@Param("recordId") Long recordId);

    WritingRecord findByIdForUser(@Param("recordId") Long recordId,
                                  @Param("userId") Long userId);

    WritingRecord findAnyByIdForUser(@Param("recordId") Long recordId,
                                     @Param("userId") Long userId);

    List<WritingRecord> findByUserId(@Param("userId") Long userId);

    void updateExtractedText(@Param("recordId") Long recordId,
                             @Param("extractedText") String extractedText);

    void updateAiResult(WritingRecord record);

    void updateAiStatus(@Param("id") Long id,
                        @Param("aiStatus") String aiStatus);

    void logicalDeleteById(@Param("id") Long id,
                           @Param("deletedTime") java.time.LocalDateTime deletedTime);

    void softDeleteById(@Param("recordId") Long recordId);

    void restoreByIdForAdmin(@Param("recordId") Long recordId);

    void softDeleteByIdForUser(@Param("recordId") Long recordId,
                               @Param("userId") Long userId);

    void restoreByIdForUser(@Param("recordId") Long recordId,
                            @Param("userId") Long userId);

    Long countUserActive(@Param("userId") Long userId,
                         @Param("query") UserWritingRecordPageQuery query);

    List<WritingRecord> pageUserActive(@Param("userId") Long userId,
                                       @Param("query") UserWritingRecordPageQuery query,
                                       @Param("offset") Integer offset,
                                       @Param("limit") Integer limit);

    Long countUserDeleted(@Param("userId") Long userId,
                          @Param("query") UserWritingDeletedRecordPageQuery query);

    List<WritingRecord> pageUserDeleted(@Param("userId") Long userId,
                                        @Param("query") UserWritingDeletedRecordPageQuery query,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    Long countAdminActive(@Param("query") AdminWritingRecordPageQuery query);

    List<WritingRecord> pageAdminActive(@Param("query") AdminWritingRecordPageQuery query,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    Long countAdminDeleted(@Param("query") AdminWritingDeletedRecordPageQuery query);

    List<WritingRecord> pageAdminDeleted(@Param("query") AdminWritingDeletedRecordPageQuery query,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);

    List<WritingRecord> findRecentActiveByUserId(@Param("userId") Long userId,
                                                 @Param("limit") Integer limit);

    List<WritingRecord> findRecentAiFailures(@Param("limit") Integer limit);

    BigDecimal selectUserAverageScore(@Param("userId") Long userId);

    Long countAdminAiFailed();
}
