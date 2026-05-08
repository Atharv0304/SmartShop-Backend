package com.smartstore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartstoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartstoreApplication.class, args);
    }
}