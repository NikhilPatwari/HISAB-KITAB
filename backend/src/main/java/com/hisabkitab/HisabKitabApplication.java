package com.hisabkitab;

import com.hisabkitab.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class HisabKitabApplication {

    public static void main(String[] args) {
        SpringApplication.run(HisabKitabApplication.class, args);
    }
}
