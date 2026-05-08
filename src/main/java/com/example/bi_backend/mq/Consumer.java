package com.example.bi_backend.mq;


import com.example.bi_backend.model.enums.ChartStatus;
import com.example.bi_backend.common.ErrorCode;
import com.example.bi_backend.constant.BiMqConstant;
import com.example.bi_backend.exception.BusinessException;
import com.example.bi_backend.manager.AIManager;
import com.example.bi_backend.model.entity.Chart;
import com.example.bi_backend.service.ChartService;
import com.example.bi_backend.utils.ChartCleanUtils;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Consumer {

    @Resource
    private ChartService chartService;

    @Resource
    private AIManager aiManager;


    @SneakyThrows
    @RabbitListener(queues = {BiMqConstant.BI_QUEUE_NAME}, ackMode = "MANUAL")
    public void receiveHandler(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) {

        //三道筛选

        if (StringUtils.isBlank(message)) {
            channel.basicNack(tag, false, false);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "消息为空");
        }

        long chartId = Long.parseLong(message);
        Chart chart=chartService.getById(chartId);
        if (chart == null) {
            channel.basicNack(tag, false, false);
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"图表为空");
        }

        //更改状态
        Chart updateChart = new Chart();
        updateChart.setId(chart.getId());
        updateChart.setStatus(ChartStatus.RUNNING.getValue());
        boolean b = chartService.updateById(updateChart);
        if (!b) {
            channel.basicNack(tag, false, false);
            handleUpdateChartError(chart.getId(), "更新图表执行中状态失败");
            return;
        }

        //调用AI
        String AIResult = aiManager.doBiChat(chart.getGoal(),chart.getChartType(),chart.getChartData());

        //System.out.println("🚨 照妖镜！AI返回的原始内容是：\n" + AIResult);

        //梳理答案，准备传输
        String[] splits = AIResult.split("【【【【【");
        if (splits.length < 3) {
            channel.basicNack(tag, false, false);
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
            channel.basicNack(tag, false, false);
            handleUpdateChartError(chart.getId(), "更新图表成功状态失败");
        }


        channel.basicAck(tag, false);
    }


    private void handleUpdateChartError(long chartId, String execMessage) {
        Chart updateChart = new Chart();
        updateChart.setId(chartId);
        updateChart.setStatus(ChartStatus.FAILED.getValue());
        updateChart.setExecMessage(execMessage);
        boolean result = chartService.updateById(updateChart);
        if (!result) {
            log.error("更新图表失败状态：失败{},{}", chartId, execMessage);
        }

    }

}
