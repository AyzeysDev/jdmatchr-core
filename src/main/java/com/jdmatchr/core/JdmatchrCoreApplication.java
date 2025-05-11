package com.jdmatchr.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class JdmatchrCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(JdmatchrCoreApplication.class, args);
		System.out.println("JDMatchr Core Spring Boot Application has started!");
	}

}
