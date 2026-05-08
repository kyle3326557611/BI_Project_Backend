package com.example.bi_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.bi_backend.model.dto.chart.ChartQueryRequest;

import com.example.bi_backend.model.entity.Chart;
import com.example.bi_backend.service.ChartService;
import com.example.bi_backend.mapper.ChartMapper;
import org.springframework.stereotype.Service;

/**
 * @author kyle
 *
 */


@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
        implements ChartService {

    @Override
    public QueryWrapper<Chart> getQueryWrapper(ChartQueryRequest chartQueryRequest) {
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        if (chartQueryRequest == null) {
            return queryWrapper;
        }

        //从前端请求中拿取对应的关键字
        Long id = chartQueryRequest.getId();
        String goal = chartQueryRequest.getGoal();
        String cname = chartQueryRequest.getCname();
        String chartType = chartQueryRequest.getChartType();
        Long userId = chartQueryRequest.getUserId();

        //判断是否插入+数据库字段+从前端获取的请求
        queryWrapper.eq(ObjectUtils.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjectUtils.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StringUtils.isNotBlank(goal), "goal", goal);
        queryWrapper.like(StringUtils.isNotBlank(cname), "cname", cname);
        queryWrapper.eq(StringUtils.isNotEmpty(chartType), "chartType", chartType);

        queryWrapper.orderByDesc("createTime");

        return queryWrapper;
    }

}


