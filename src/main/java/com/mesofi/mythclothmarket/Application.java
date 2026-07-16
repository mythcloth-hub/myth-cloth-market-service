package com.mesofi.mythclothmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point for the Myth Cloth market service.
 */
@SpringBootApplication
public class Application {

    /**
     * Bootstraps the Spring application context.
     *
     * @param args
     *            optional command-line arguments.
     */
    static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
