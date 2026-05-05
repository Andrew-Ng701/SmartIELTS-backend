package com.andrew.smartielts.dashboard.learning.mapper;

import com.andrew.smartielts.dashboard.learning.dto.LearningObjectDTO;
import com.andrew.smartielts.dashboard.learning.dto.ModuleLearningContextDTO;
import com.andrew.smartielts.dashboard.learning.dto.UserAttemptDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface LearningObjectMapper {

    LearningObjectDTO selectListeningQuestion(@Param("questionId") Long questionId);

    LearningObjectDTO selectReadingQuestion(@Param("questionId") Long questionId);

    LearningObjectDTO selectSpeakingQuestion(@Param("questionId") Long questionId);

    LearningObjectDTO selectWritingQuestion(@Param("questionId") Long questionId);

    LearningObjectDTO selectReadingPassage(@Param("passageId") Long passageId);

    LearningObjectDTO selectListeningTest(@Param("testId") Long testId);

    LearningObjectDTO selectReadingTest(@Param("testId") Long testId);

    UserAttemptDTO selectListeningAttempt(@Param("userId") Long userId,
                                          @Param("recordId") Long recordId,
                                          @Param("questionId") Long questionId);

    UserAttemptDTO selectReadingAttempt(@Param("userId") Long userId,
                                        @Param("recordId") Long recordId,
                                        @Param("questionId") Long questionId);

    UserAttemptDTO selectSpeakingAttempt(@Param("userId") Long userId,
                                         @Param("recordId") Long recordId,
                                         @Param("questionId") Long questionId);

    UserAttemptDTO selectWritingAttempt(@Param("userId") Long userId,
                                        @Param("recordId") Long recordId,
                                        @Param("questionId") Long questionId);

    List<Map<String, Object>> selectListeningRecordQuestions(@Param("userId") Long userId,
                                                             @Param("recordId") Long recordId);

    List<Map<String, Object>> selectReadingRecordQuestions(@Param("userId") Long userId,
                                                           @Param("recordId") Long recordId);

    Map<String, Object> locateListeningQuestionByNumber(@Param("userId") Long userId,
                                                        @Param("recordId") Long recordId,
                                                        @Param("questionNumber") Integer questionNumber);

    Map<String, Object> locateReadingQuestionByNumber(@Param("userId") Long userId,
                                                      @Param("recordId") Long recordId,
                                                      @Param("questionNumber") Integer questionNumber);

    ModuleLearningContextDTO selectListeningContext(@Param("userId") Long userId,
                                                    @Param("recordId") Long recordId,
                                                    @Param("questionId") Long questionId);

    ModuleLearningContextDTO selectReadingContext(@Param("userId") Long userId,
                                                  @Param("recordId") Long recordId,
                                                  @Param("questionId") Long questionId);

    ModuleLearningContextDTO selectWritingContext(@Param("userId") Long userId,
                                                  @Param("recordId") Long recordId);

    ModuleLearningContextDTO selectSpeakingContext(@Param("userId") Long userId,
                                                   @Param("recordId") Long recordId);
}