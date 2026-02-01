package com.p2.isitout;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IsItOutYetApplication {

	public static void main(String[] args) {
		SpringApplication.run(IsItOutYetApplication.class, args);
	}

}
