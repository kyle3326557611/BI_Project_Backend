package com.example.bi_backend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 自定义返回类
 *
 * @param <T>
 */

@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

}
