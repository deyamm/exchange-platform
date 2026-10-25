package com.exchange.platform.analytics;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.exchange.platform.analytics.mapper")
@EnableCaching
public class DataAnalyticsApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataAnalyticsApplication.class, args);
    }
}
