package com.example.bi_backend.model.vo;

import lombok.Data;

@Data
public class BIResponseVO {

    /**
     * AI生成图表
     */
    private String genChart;

    /**
     * AI生成结论
     */
    private String genResult;

    /**
     * 图表ID
     */
    private Long chartId;
}
