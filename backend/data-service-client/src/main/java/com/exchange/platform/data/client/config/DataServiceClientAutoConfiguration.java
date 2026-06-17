package com.exchange.platform.data.client.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

import com.exchange.platform.data.client.DataServiceClient;

/**
 * data-service-client 自动配置。
 * resources下的文件和该配置文件的作用是让其他服务引入 data-service-client 依赖后，能够自动配置好 FeignClient，直接注入 DataServiceClient 就能使用。
 * 
 * 省去了在每个服务中都要写一个 @EnableFeignClients 注解的麻烦，并且通过 basePackageClasses 定义了扫描 DataServiceClient 接口所在的包，确保 FeignClient 能被正确识别和注册。
 */
@Configuration(proxyBeanMethods = false)
@EnableFeignClients(basePackageClasses = DataServiceClient.class)
public class DataServiceClientAutoConfiguration {
}
