package com.task.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ApiWorkerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiWorkerApplication.class, args);
	}

}
