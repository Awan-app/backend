package com.ezdo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling
public class EzdoApplication {

	public static void main(String[] args) {
		SpringApplication.run(EzdoApplication.class, args);
	}

}
