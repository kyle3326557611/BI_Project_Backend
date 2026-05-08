package com.example.bi_backend.common;

import com.example.bi_backend.constant.CommonConstant;
import lombok.Data;

/**
 * 分页查询属性（默认数据，可覆盖）
 */
@Data
public class PageRequest {
    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认升序）
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}
