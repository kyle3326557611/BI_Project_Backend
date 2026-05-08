package com.example.bi_backend.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Excel相关工具类
 */
@Slf4j
public class ExcelUtils {

    public static String excelToCSV(MultipartFile multipartFile) {

        //先读取数据
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()  //读取第一页
                    .headRowNumber(0)  //从第0行开始读取，不要跳过表头
                    .doReadSync();  //同步读取
        } catch (IOException e) {
            log.error("表格处理错误", e);
        }
        //如果是空表格
        if (CollUtil.isEmpty(list)) {
            return " ";
        }
        //转换成CSV格式
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < list.size(); i++) {
            //既要维护键值对，又要维护插入顺序
            LinkedHashMap<Integer, String> dataMap = new LinkedHashMap<>(list.get(i));
            //filter表示筛选器（即筛选出空格）
            List<String> dataList = dataMap.values().stream().filter(ObjectUtils::isNotEmpty).toList();
            stringBuilder.append(StringUtils.join(dataList, ",")).append("\n");
        }

        return stringBuilder.toString();
    }
}
