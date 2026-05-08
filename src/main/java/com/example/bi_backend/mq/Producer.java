package com.example.bi_backend.mq;

import com.example.bi_backend.constant.BiMqConstant;
import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class Producer {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void send(String message) {

        rabbitTemplate.convertAndSend(BiMqConstant.BI_EXCHANGE_NAME, BiMqConstant.BI_ROUTING_KEY, message);
    }
}
