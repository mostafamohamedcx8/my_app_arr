package com.myapp.aqdeeha;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AqdeehaApplication {

	public static void main(String[] args) {
		SpringApplication.run(AqdeehaApplication.class, args);
	}

}
