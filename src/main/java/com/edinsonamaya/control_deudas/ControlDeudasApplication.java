package com.edinsonamaya.control_deudas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ControlDeudasApplication {

	public static void main(String[] args) {
		SpringApplication.run(ControlDeudasApplication.class, args);
	}

}
