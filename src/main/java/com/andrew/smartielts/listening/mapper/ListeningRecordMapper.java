package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningRecord;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.admin.AdminListeningRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningDeletedRecordPageQuery;
import com.andrew.smartielts.listening.domain.query.user.UserListeningRecordPageQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ListeningRecordMapper {

    Long countUserActive(@Param("userId") Long userId,
                         @Param("query") UserListeningRecordPageQuery query);

    List<ListeningRecord> pageUserActive(@Param("userId") Long userId,
                                         @Param("query") UserListeningRecordPageQuery query,
                                         @Param("offset") Integer offset,
                                         @Param("limit") Integer limit);

    Long countUserDeleted(@Param("userId") Long userId,
                          @Param("query") UserListeningDeletedRecordPageQuery query);

    List<ListeningRecord> pageUserDeleted(@Param("userId") Long userId,
                                          @Param("query") UserListeningDeletedRecordPageQuery query,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    Long countAdminActive(@Param("query") AdminListeningRecordPageQuery query);

    List<ListeningRecord> pageAdminActive(@Param("query") AdminListeningRecordPageQuery query,
                                          @Param("offset") Integer offset,
                                          @Param("limit") Integer limit);

    Long countAdminDeleted(@Param("query") AdminListeningDeletedRecordPageQuery query);

    List<ListeningRecord> pageAdminDeleted(@Param("query") AdminListeningDeletedRecordPageQuery query,
                                           @Param("offset") Integer offset,
                                           @Param("limit") Integer limit);

    ListeningRecord findAnyById(@Param("id") Long id);

    ListeningRecord findAnyByIdForUser(@Param("recordId") Long recordId,
                                       @Param("userId") Long userId);

    int insert(ListeningRecord record);

    int updateTotalScore(@Param("recordId") Long recordId,
                         @Param("totalScore") Integer totalScore);

    int softDeleteById(@Param("recordId") Long recordId);

    int restoreById(@Param("recordId") Long recordId);

    int softDeleteByIdForUser(@Param("recordId") Long recordId,
                              @Param("userId") Long userId);

    int restoreByIdForUser(@Param("recordId") Long recordId,
                           @Param("userId") Long userId);

    List<ListeningRecord> findRecentActiveByUserId(@Param("userId") Long userId,
                                                   @Param("limit") Integer limit);

    BigDecimal selectUserAverageScore(@Param("userId") Long userId);

    ListeningRecord findBySessionIdForUser(@Param("sessionId") String sessionId,
                                           @Param("userId") Long userId);

    ListeningRecord findInProgressByTestIdForUser(@Param("testId") Long testId,
                                                  @Param("userId") Long userId);

    int updateSessionState(ListeningRecord record);
}