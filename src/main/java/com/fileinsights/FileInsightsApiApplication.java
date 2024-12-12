package com.fileinsights;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan; // Updated import

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.fileinsights.repository") // Enable repositories in this package
@EntityScan("com.fileinsights.entity") // Specify where Spring should scan for JPA entities
public class FileInsightsApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FileInsightsApiApplication.class, args);
    }
}
