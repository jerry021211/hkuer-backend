package com.hkuer.hkuer;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.hkuer.hkuer.mapper")
public class HkuerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HkuerApplication.class, args);
    }

}
