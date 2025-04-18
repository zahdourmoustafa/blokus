package com.blokus.blokus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class BlokusApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlokusApplication.class, args);
	}

}
