package com.example.bi_backend.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * 清洗AI结果
 */
public class ChartCleanUtils {
    public static String cleanGenChart(String genChart) {
        if (StringUtils.isBlank(genChart)) {
            return genChart;
        }
        // 1. 强行截取 { 和 } 之间的内容
        int start = genChart.indexOf("{");
        int end = genChart.lastIndexOf("}");
        if (start != -1 && end != -1) {
            genChart = genChart.substring(start, end + 1); //前闭后开区间
        }
        // 2. 正则替换大模型可能多加的前导空格
        genChart = genChart.replaceAll("\"\\s+xAxis\"", "\"xAxis\"");
        genChart = genChart.replaceAll("\"\\s+yAxis\"", "\"yAxis\"");
        return genChart.trim();
    }
}
