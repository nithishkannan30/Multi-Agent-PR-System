package com.example.MultiAgentsForPR;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class MultiAgentsForPrApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultiAgentsForPrApplication.class, args);
	}

}
