package com.nevin.incidentflow;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
public class IncidentflowApplication {

    private static final Logger log = LoggerFactory.getLogger(IncidentflowApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IncidentflowApplication.class, args);
    }

    @Bean
    CommandLineRunner logRole(@Value("${incidentflow.role:UNKNOWN}") String role) {
        return args -> log.info(">>> IncidentFlow started in role: {}", role);
    }
}
