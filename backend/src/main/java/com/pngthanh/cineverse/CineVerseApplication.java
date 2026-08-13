package com.pngthanh.cineverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CineVerseApplication {
    public static void main(String[] args) {
        SpringApplication.run(CineVerseApplication.class, args);
    }
}
