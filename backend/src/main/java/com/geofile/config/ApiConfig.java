package com.geofile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * API配置类
 * 配置RestTemplate用于HTTP请求
 */
@Configuration
public class ApiConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
