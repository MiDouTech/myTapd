package com.miduo.cloud.ticket.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 工单系统主启动类
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.miduo.cloud.ticket")
@EnableAsync
public class TicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(TicketApplication.class, args);
    }
}
