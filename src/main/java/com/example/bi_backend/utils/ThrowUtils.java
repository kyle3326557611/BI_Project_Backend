package com.example.bi_backend.utils;


import com.example.bi_backend.common.ErrorCode;
import com.example.bi_backend.exception.BusinessException;

/**
 * 自定义异常工具类
 */
public class ThrowUtils {

    public static void throwIf(boolean condition, RuntimeException exception) {
        if (condition) {
            throw exception;
        }
    }

    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
