package com.token_service;

import com.token_service.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
@EnableScheduling
public class TokenServiceApplication {
	public static void main(String[] args) {
		SpringApplication.run(TokenServiceApplication.class, args);
	}
}
