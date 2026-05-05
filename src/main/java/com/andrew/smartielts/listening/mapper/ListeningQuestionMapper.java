package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListeningQuestionMapper {

    int insertListeningQuestion(ListeningQuestion question);

    ListeningQuestion findActiveById(@Param("id") Long id);

    ListeningQuestion findAnyById(@Param("id") Long id);

    List<ListeningQuestion> findActiveByTestId(@Param("testId") Long testId);

    List<ListeningQuestion> findAnyByTestId(@Param("testId") Long testId);

    List<ListeningQuestion> findActiveByPartGroupId(@Param("partGroupId") Long partGroupId);

    List<ListeningQuestion> findAnyByPartGroupId(@Param("partGroupId") Long partGroupId);

    int updateListeningQuestion(ListeningQuestion question);

    int softDeleteById(@Param("id") Long id);

    int softDeleteByTestId(@Param("testId") Long testId);

    int softDeleteByPartGroupId(@Param("partGroupId") Long partGroupId);

    int restoreById(@Param("id") Long id);

    int restoreByTestId(@Param("testId") Long testId);

    int restoreByPartGroupId(@Param("partGroupId") Long partGroupId);
}