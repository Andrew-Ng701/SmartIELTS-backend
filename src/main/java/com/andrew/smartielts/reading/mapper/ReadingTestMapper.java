package com.andrew.smartielts.reading.mapper;

import com.andrew.smartielts.reading.domain.pojo.ReadingTest;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReadingTestMapper {

    void insert(ReadingTest test);

    ReadingTest findById(Long id);

    List<ReadingTest> findAll();

    void update(ReadingTest test);

    void deleteById(Long id);
}
