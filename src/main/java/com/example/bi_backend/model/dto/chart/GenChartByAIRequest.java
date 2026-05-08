package com.example.bi_backend.model.dto.chart;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 需要传递给AI的请求
 */
@Data
public class GenChartByAIRequest implements Serializable {

    /**
     * 名称
     */
    private String cname;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表类型
     */
    private String chartType;

    @Serial
    private static final long serialVersionUID = 1L;

}
