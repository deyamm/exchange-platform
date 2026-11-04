package com.exchange.platform.collection;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.exchange.platform.collection.mapper")
@EnableScheduling
public class DataCollectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DataCollectionApplication.class, args);
    }
}
