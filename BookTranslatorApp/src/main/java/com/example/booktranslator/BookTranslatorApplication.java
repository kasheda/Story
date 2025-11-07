package com.example.booktranslator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BookTranslatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookTranslatorApplication.class, args);
    }
}
