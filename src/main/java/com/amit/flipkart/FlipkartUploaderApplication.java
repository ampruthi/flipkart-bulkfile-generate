package com.amit.flipkart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class FlipkartUploaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(FlipkartUploaderApplication.class, args);
    }
}
