package com.example.spacetraderspicyber;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {"com.example.spacetraderspicyber"})
@SpringBootApplication
public class SpacetraderSpicyberApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpacetraderSpicyberApplication.class, args);
	}

}
