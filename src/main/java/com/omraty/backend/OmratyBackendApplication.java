package com.omraty.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OmratyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(OmratyBackendApplication.class, args);
    }
}
