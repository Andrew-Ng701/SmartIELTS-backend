package com.andrew.smartielts.reading.mapper;

import com.andrew.smartielts.reading.domain.pojo.ReadingPassage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ReadingPassageMapper {

    void insert(ReadingPassage passage);

    ReadingPassage findById(Long id);

    List<ReadingPassage> findByTestId(Long testId);

    void update(ReadingPassage passage);

    void deleteById(Long id);

    void deleteByTestId(Long testId);
}
