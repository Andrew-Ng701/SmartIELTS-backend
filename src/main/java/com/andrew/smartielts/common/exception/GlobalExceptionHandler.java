package com.andrew.smartielts.common.exception;

import com.andrew.smartielts.common.resultDTO.Result;
import com.andrew.smartielts.common.debug.DebugNdjsonLogger;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handleRuntimeException(RuntimeException e) {
        return Result.error(e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public Result<?> handleNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        // #region agent log
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exception", e.getClass().getName());
        data.put("message", e.getMessage());
        DebugNdjsonLogger.log("pre-fix", "H2", "GlobalExceptionHandler.java:handleNotAcceptable", "media type not acceptable", data);
        // #endregion
        return Result.error("Not Acceptable: set Accept=application/json");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<?> handleNotSupported(HttpMediaTypeNotSupportedException e) {
        // #region agent log
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("exception", e.getClass().getName());
        data.put("message", e.getMessage());
        data.put("contentType", e.getContentType() == null ? null : e.getContentType().toString());
        DebugNdjsonLogger.log("pre-fix", "H3", "GlobalExceptionHandler.java:handleNotSupported", "media type not supported", data);
        // #endregion
        return Result.error("Unsupported Media Type: use multipart/form-data");
    }

}