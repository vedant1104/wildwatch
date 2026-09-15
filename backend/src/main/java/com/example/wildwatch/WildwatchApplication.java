package com.example.wildwatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class WildwatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(WildwatchApplication.class, args);
	}

}
