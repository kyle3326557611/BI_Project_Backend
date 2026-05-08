package com.example.bi_backend.config;


import com.example.bi_backend.constant.BiMqConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMq配置类
 */
@Configuration
public class RabbitMqConfig {

    /**
     *消费者队列配置
     */
    @Bean
    public Queue queue() {
        //持久化，重启后任务不会丢失
        return new Queue(BiMqConstant.BI_QUEUE_NAME,true);
    }

    /**
     * 交换机配置
     * 在这里选择是直连（Direct），还是扇出（Fanout），还是主题（Topic）
     */
    @Bean
    public DirectExchange exchange() {
        //1.持久化
        //2.自动删除
        return new DirectExchange(BiMqConstant.BI_EXCHANGE_NAME,true,false);
    }

    /**
     * 连接
     */
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(queue()).to(exchange()).with(BiMqConstant.BI_ROUTING_KEY);
    }
}
