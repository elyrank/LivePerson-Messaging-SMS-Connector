package com.liveperson.tutorial.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author elyran
 * @since 5/30/16.
 */
@SpringBootApplication
@ComponentScan("com.liveperson.tutorial.ws")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}