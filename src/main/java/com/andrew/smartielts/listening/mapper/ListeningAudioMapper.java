package com.andrew.smartielts.listening.mapper;

import com.andrew.smartielts.listening.domain.pojo.ListeningAudio;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ListeningAudioMapper {

    int insertListeningAudio(ListeningAudio audio);

    ListeningAudio findById(@Param("id") Long id);

    ListeningAudio findTestAudioByTestId(@Param("testId") Long testId);

    List<ListeningAudio> findByTestId(@Param("testId") Long testId);

    List<ListeningAudio> findByPartGroupId(@Param("partGroupId") Long partGroupId);

    int updateListeningAudio(ListeningAudio audio);

    int deleteById(@Param("id") Long id);

    int deleteByTestId(@Param("testId") Long testId);

    int deleteByPartGroupId(@Param("partGroupId") Long partGroupId);
}