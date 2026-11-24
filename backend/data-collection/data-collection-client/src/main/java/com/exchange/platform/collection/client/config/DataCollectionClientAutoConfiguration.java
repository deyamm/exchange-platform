package com.exchange.platform.collection.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import com.exchange.platform.collection.client.feign.DataCollectionClient;

/**
 * data-collection-client 自动配置，供引入该客户端的服务直接注入 Feign 接口。
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = DataCollectionClient.class)
public class DataCollectionClientAutoConfiguration {
}
