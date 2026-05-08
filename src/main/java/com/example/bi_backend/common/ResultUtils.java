package com.example.bi_backend.common;

/**
 * 返回工具类
 *
 */
public class ResultUtils {

    /**
     * 成功
     *
     * @param data 结果
     * @param <T>  类型参数化
     * @return 成功参数
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, data, "ok");
    }

    /**
     * 失败
     *
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new <T>BaseResponse<T>(code, null, message);
    }

    /**
     * 失败
     *
     * @param errorCode 错误码
     * @return 返回参数
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new <T>BaseResponse<T>(errorCode.getCode(), null, message);
    }
}
