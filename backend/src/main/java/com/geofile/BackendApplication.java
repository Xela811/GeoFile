package com.geofile;

import jakarta.annotation.PostConstruct;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@MapperScan("com.geofile.mapper")
@ComponentScan("com.geofile")  // 明确指定扫描包
@EnableScheduling
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }
    @PostConstruct
    void started() {
        // 这一行能解决 99% 的 MyBatis/JDBC 时区解析偏移问题
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+8"));
    }
}
