package com.example.bi_backend.model.dto.chart;

import com.example.bi_backend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

/**
 * 前端传入图表信息（查询图表）
 */

@EqualsAndHashCode (callSuper = true)
@Data
public class ChartQueryRequest extends PageRequest implements Serializable {

    /**
     * 图表id
     */
    private Long id;

    /**
     * 分析目标
     */
    private String goal;

    /**
     * 图表名称
     */
    private String cname;

    /**
     * 图表类型
     */
    private String chartType;

    /**
     * 创建图表的角色
     */
    private Long userId;

    @Serial
    private static final long serialVersionUID = 1L;
}