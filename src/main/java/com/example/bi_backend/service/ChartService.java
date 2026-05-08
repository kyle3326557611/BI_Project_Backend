package com.example.bi_backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.bi_backend.model.dto.chart.ChartQueryRequest;
import com.example.bi_backend.model.entity.Chart;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author kyle
*/
public interface ChartService extends IService<Chart> {

    QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest);


}
