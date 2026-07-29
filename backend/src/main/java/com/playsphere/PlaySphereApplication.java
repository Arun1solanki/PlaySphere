package com.playsphere;

import com.playsphere.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class PlaySphereApplication {
    public static void main(String[] args) {
        SpringApplication.run(PlaySphereApplication.class, args);
    }
}
