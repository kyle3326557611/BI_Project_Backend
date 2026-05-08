package com.example.bi_backend.model.enums;

import lombok.Getter;

/**
 * 图表状态
 */
@Getter
public enum ChartStatus {

    WAIT("wait", "等待中"),
    RUNNING("running", "运行中"),
    SUCCEED("succeed", "已成功"),
    FAILED("failed", "已失败");

    private final String value;
    private final String message;

    ChartStatus(String value, String message) {
        this.value = value;
        this.message = message;
    }


}
