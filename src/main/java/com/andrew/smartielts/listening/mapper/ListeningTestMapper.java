package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningTest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ListeningTestMapper {
    void insertListeningTest(ListeningTest test);
    ListeningTest findById(Long id);
    List<ListeningTest> findAll();
    void updateListeningTest(ListeningTest test);
    void deleteById(Long id);
}
