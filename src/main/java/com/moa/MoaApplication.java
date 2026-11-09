package com.moa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan    // moa 아래에서 찾으면, JwtProperties 발견
public class MoaApplication {

	public static void main(String[] args) {
        SpringApplication.run(MoaApplication.class, args);
	}

}
