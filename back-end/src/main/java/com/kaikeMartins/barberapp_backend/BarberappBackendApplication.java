package com.kaikeMartins.barberapp_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class BarberappBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BarberappBackendApplication.class, args);
	}

}
