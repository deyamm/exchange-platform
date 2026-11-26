package com.exchange.platform.analytics.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import com.exchange.platform.analytics.client.MasterDataClient;

@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = MasterDataClient.class)
public class MasterDataClientAutoConfiguration {
}
