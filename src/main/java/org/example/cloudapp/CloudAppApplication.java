package org.example.cloudapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class CloudAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudAppApplication.class, args);
    }

}
