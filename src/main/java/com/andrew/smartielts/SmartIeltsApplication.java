package com.andrew.smartielts;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.andrew.smartielts.auth.mapper")
public class SmartIeltsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartIeltsApplication.class, args);
    }

}
