package com.exchange.platform.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.exchange.platform.analytics.mapper")
public class DataAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataAnalyticsApplication.class, args);
    }
}
