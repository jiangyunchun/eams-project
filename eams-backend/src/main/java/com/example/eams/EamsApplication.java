package com.example.eams;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * EAMS企业资产管理系统 - 启动类
 */
@SpringBootApplication
@MapperScan("com.example.eams.**.mapper")
@EnableScheduling
public class EamsApplication {

    public static void main(String[] args) {
        SpringApplication.run(EamsApplication.class, args);
    }
}
