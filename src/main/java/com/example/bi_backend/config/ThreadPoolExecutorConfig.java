package com.example.bi_backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolExecutorConfig {
    ThreadFactory threadFactory = new ThreadFactory() {

        private int count = 1;

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setName("线程-" + count);
            count++;
            return t;
        }

    };

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(2, 4, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(4), threadFactory);
    }
}
