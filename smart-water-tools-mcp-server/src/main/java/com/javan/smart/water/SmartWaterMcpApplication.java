package com.javan.smart.water;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动类
 */
@SpringBootApplication
@EnableAsync
public class SmartWaterMcpApplication {

    /**
     * 启动
     *
     * @param args 参数
     */
    public static void main(String[] args) {
        SpringApplication.run(SmartWaterMcpApplication.class, args);
    }
}
