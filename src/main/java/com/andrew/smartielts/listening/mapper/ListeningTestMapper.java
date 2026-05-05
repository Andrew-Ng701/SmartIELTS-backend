package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListeningTestMapper {

    int insertListeningTest(ListeningTest test);

    ListeningTest findActiveById(@Param("id") Long id);

    ListeningTest findAnyById(@Param("id") Long id);

    List<ListeningTest> findAllActive();

    List<ListeningTest> findAllDeleted();

    List<ListeningTest> findAllIncludingDeleted();

    int updateListeningTest(ListeningTest test);

    int softDeleteById(@Param("id") Long id);

    int restoreById(@Param("id") Long id);
}