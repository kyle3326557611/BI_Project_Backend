package com.example.bi_backend.model.dto.chart;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 前端传入图表信息（添加图表）
 *
 */
@Data
public class ChartAddRequest implements Serializable {

    /**
     * 图表名称
     */
    private String cname;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表数据 (CSV格式)
     */
    private String chartData;

    /**
     * 图表类型
     */
    private String chartType;

    @Serial
    private static final long serialVersionUID = 1L;
}
