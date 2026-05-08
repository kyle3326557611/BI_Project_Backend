package com.example.bi_backend.controller;


import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.bi_backend.common.*;
import com.example.bi_backend.exception.BusinessException;
import com.example.bi_backend.model.dto.common.DeleteRequest;
import com.example.bi_backend.model.enums.ChartStatus;
import com.example.bi_backend.utils.ChartCleanUtils;
import com.example.bi_backend.utils.ThrowUtils;
import com.example.bi_backend.manager.AIManager;
import com.example.bi_backend.manager.RedisLimiterManager;
import com.example.bi_backend.model.dto.chart.ChartAddRequest;
import com.example.bi_backend.model.dto.chart.ChartQueryRequest;
import com.example.bi_backend.model.dto.chart.GenChartByAIRequest;
import com.example.bi_backend.model.entity.Chart;
import com.example.bi_backend.model.entity.User;
import com.example.bi_backend.model.vo.BIResponseVO;
import com.example.bi_backend.mq.Producer;
import com.example.bi_backend.service.ChartService;
import com.example.bi_backend.service.UserService;
import com.example.bi_backend.utils.ExcelUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 图表控制层
 *
 * @author kyle
 */

@RestController
@RequestMapping("/chart")
@Slf4j
public class ChartController {

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private AIManager aiManager;

    @Resource
    private RedisLimiterManager redisLimiterManager;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private Producer producer;

    /**
     * 增加图表
     */
    @PostMapping("/add")
    public BaseResponse<Long> addChart(@RequestBody ChartAddRequest chartAddRequest, HttpServletRequest request) {
        if (chartAddRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //将DTO类转换成Entity
        Chart chart = new Chart();
        BeanUtils.copyProperties(chartAddRequest, chart);

        //将ID从Service中获取而不是从前端获取
        User loginUser = userService.getLoginUser(request);
        chart.setUserId(loginUser.getId());

        //将数据传入数据库
        boolean result = chartService.save(chart);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        long newChartId = chart.getId();
        return ResultUtils.success(newChartId);
    }


    /**
     * 删除图表
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteChart(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        //检查实体是否存在
        Long chartId = deleteRequest.getId();
        Chart chart = chartService.getById(chartId);
        ThrowUtils.throwIf(chart == null, ErrorCode.NOT_FOUND_ERROR);
        //权限校验
        User user = userService.getLoginUser(request);
        if (!chart.getUserId().equals(user.getId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        boolean b = chartService.removeById(chartId);
        return ResultUtils.success(b);
    }

    /**
     * 根据id获取对应详细图表
     */
    @PostMapping("/get")
    public BaseResponse<Chart> getChartById(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Chart chart = chartService.getById(id);
        if (chart == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        //鉴权
        User loginUser = userService.getLoginUser(request);
        if (!loginUser.getId().equals(chart.getUserId()) && !userService.isAdmin(request)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限访问");
        }

        return ResultUtils.success(chart);
    }

    /**
     * 获取个人创建的图表列表(查看历史查询)
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<Chart>> listChartByPage(@RequestBody ChartQueryRequest chartQueryRequest, HttpServletRequest request) {
        if (chartQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        long current = chartQueryRequest.getCurrent();  //当前页数
        long size = chartQueryRequest.getPageSize();  //每页显示的数量

        //限制输出太多导致内存溢出
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        //鉴权
        User user = userService.getLoginUser(request);
        chartQueryRequest.setUserId(user.getId());

        //页数+条件，这里的查询条件从前端拿取
        //以chart为类型的Page
        Page<Chart> chartPage = chartService.page(new Page<>(current, size), chartService.getQueryWrapper(chartQueryRequest));

        return ResultUtils.success(chartPage);
    }


    /**
     * 智能分析(异步消息队列)
     *
     */
    @PostMapping("/gen/async/mq")
    public BaseResponse<BIResponseVO> genChartByAIAsyncMq(@RequestPart("file") MultipartFile multipartFile,
                                                          @org.springdoc.core.annotations.ParameterObject GenChartByAIRequest genChartByAIRequest,
                                                          HttpServletRequest request) {

        String name = genChartByAIRequest.getCname();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();

        User user = userService.getLoginUser(request);

        this.validateChartRequest(goal, name, multipartFile);

        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId());

        // 将原始数据进行压缩
        String csvData = ExcelUtils.excelToCSV(multipartFile);

        //插入数据到数据库
        Chart chart = new Chart();

        chart.setCname(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setStatus(ChartStatus.WAIT.getValue());
        chart.setUserId(user.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        long newChartId = chart.getId();
        //调用生产者
        producer.send(String.valueOf(newChartId));

        BIResponseVO biResponseVO = new BIResponseVO();
        biResponseVO.setChartId(newChartId);
        return ResultUtils.success(biResponseVO);

    }


    /**
     * 智能分析(异步)
     *
     */
    @PostMapping("/gen/async")
    public BaseResponse<BIResponseVO> genChartByAIAsync(@RequestPart("file") MultipartFile multipartFile,
                                                        @org.springdoc.core.annotations.ParameterObject GenChartByAIRequest genChartByAIRequest,
                                                        HttpServletRequest request) {

        String name = genChartByAIRequest.getCname();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();


        User user = userService.getLoginUser(request);
        this.validateChartRequest(goal, name, multipartFile);
        User loginUser = userService.getLoginUser(request);
        redisLimiterManager.doRateLimit("genChartByAI_" + loginUser.getId());
        String csvData = ExcelUtils.excelToCSV(multipartFile);

        //插入数据到数据库
        Chart chart = new Chart();

        chart.setCname(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setStatus(ChartStatus.WAIT.getValue());
        chart.setUserId(user.getId());
        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        try {
            CompletableFuture.runAsync(() -> {

                        Chart updateChart = new Chart();
                        updateChart.setId(chart.getId());
                        //更改状态
                        updateChart.setStatus(ChartStatus.RUNNING.getValue());
                        boolean b = chartService.updateById(updateChart);
                        if (!b) {
                            handleUpdateChartError(chart.getId(), "更新图表执行中状态失败");
                            return;
                        }
                        //调用AI
                        String AIResult = aiManager.doBiChat(goal, chartType, csvData);

                        //System.out.println("🚨 照妖镜！AI返回的原始内容是：\n" + AIResult);

                        String[] splits = AIResult.split("【【【【【");
                        if (splits.length < 3) {
                            handleUpdateChartError(chart.getId(), "AI 生成错误");
                            return;
                        }

                        String genChart = ChartCleanUtils.cleanGenChart(splits[1]);
                        String genResult = splits[2].trim();

                        //再次更改状态
                        Chart updateChart2 = new Chart();
                        updateChart2.setId(chart.getId());
                        updateChart2.setGenChart(genChart);
                        updateChart2.setGenResult(genResult);
                        updateChart2.setStatus(ChartStatus.SUCCEED.getValue());
                        boolean result = chartService.updateById(updateChart2);
                        if (!result) {
                            handleUpdateChartError(chart.getId(), "更新图表成功状态失败");
                        }

                    }, threadPoolExecutor)
                    .exceptionally(ex -> {
                        log.error("图表生成失败", ex);
                        handleUpdateChartError(chart.getId(), "AI调用超时，请重试");
                        return null;
                    });
        } catch (RejectedExecutionException e) {
            log.error("当前线程池已满，请稍后重试！", e);
            handleUpdateChartError(chart.getId(), "当前请求已满，请稍后重试！");
        }

        BIResponseVO biResponseVO = new BIResponseVO();
        biResponseVO.setChartId(chart.getId());
        return ResultUtils.success(biResponseVO);

    }

    private void handleUpdateChartError(long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(ChartStatus.FAILED.getValue());
        //错误信息传入数据库
        updateChart.setExecMessage(execMessage);
        boolean result = chartService.updateById(updateChart);
        if (!result) {
            log.error("更新图表失败状态：失败{},{}", chartId, execMessage);
        }

    }


    /**
     * 智能分析(同步)
     *
     */
    @PostMapping("/gen")
    public BaseResponse<BIResponseVO> genChartByAI(@RequestPart("file") MultipartFile multipartFile,
                                                   @org.springdoc.core.annotations.ParameterObject GenChartByAIRequest genChartByAIRequest,
                                                   HttpServletRequest request) {

        String name = genChartByAIRequest.getCname();
        String goal = genChartByAIRequest.getGoal();
        String chartType = genChartByAIRequest.getChartType();

        //先获取用户的登录态
        User user = userService.getLoginUser(request);

        // 校验文件的合法合规
        this.validateChartRequest(goal, name, multipartFile);

        //限制用户频繁请求
        redisLimiterManager.doRateLimit("genChartByAI_" + user.getId());

        // 将原始数据进行压缩
        String csvData = ExcelUtils.excelToCSV(multipartFile);

        // 调用 AI
        String AIResult = aiManager.doBiChat(goal, chartType, csvData);

        String[] splits = AIResult.split("【【【【【");
        if (splits.length < 3) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "AI生成错误");
        }
        // 拿到原始 chart 代码，清洗AI输出内容
        String genChart = ChartCleanUtils.cleanGenChart(splits[1]);  //图表清洗
        String genResult = splits[2].trim();  //结论不用洗
        // 插入数据到数据库
        Chart chart = new Chart();
        chart.setCname(name);
        chart.setGoal(goal);
        chart.setChartType(chartType);
        chart.setChartData(csvData);
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setUserId(user.getId());
        chart.setStatus(ChartStatus.SUCCEED.getValue());

        boolean saveResult = chartService.save(chart);
        ThrowUtils.throwIf(!saveResult, ErrorCode.SYSTEM_ERROR, "图表保存失败");

        //取出数据库中的数据返回前端
        BIResponseVO biResponseVO = new BIResponseVO();
        biResponseVO.setGenChart(genChart);
        biResponseVO.setGenResult(genResult);
        biResponseVO.setChartId(chart.getId());

        return ResultUtils.success(biResponseVO);
    }

    /**
     * 用来校验前端传来的图表参数和文件
     *
     * @param goal          分析目标
     * @param name          图表名称
     * @param multipartFile 用户上传的 Excel 文件
     */
    private void validateChartRequest(String goal, String name, MultipartFile multipartFile) {
        // 1. 校验文本参数(目标和图表名)
        ThrowUtils.throwIf(StringUtils.isBlank(goal), ErrorCode.PARAMS_ERROR, "目标为空");
        ThrowUtils.throwIf(StringUtils.isNotBlank(name) && name.length() >= 100, ErrorCode.PARAMS_ERROR, "名称过长");

        // 2. 校验文件属性 (防恶意注入和超大文件)
        long size = multipartFile.getSize();
        final long ONE_MB = 1024 * 1024L;
        // 限制文件大小为 1MB
        ThrowUtils.throwIf(size > ONE_MB, ErrorCode.PARAMS_ERROR, "文件过大");

        // 3. 校验文件后缀 (只允许 xlsx 或 xls)
        String originalName = multipartFile.getOriginalFilename();
        String suffix = FileUtil.getSuffix(originalName);  //获取文件扩展名
        final List<String> validFiledSuffixList = Arrays.asList("xlsx", "xls");
        ThrowUtils.throwIf(!validFiledSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "文件后缀非法");
    }


}