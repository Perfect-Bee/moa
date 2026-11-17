package com.moa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan    // moa 아래에서 찾으면, JwtProperties 발견
@EnableScheduling               // 공동구매 마감 스케줄러
public class MoaApplication {

	public static void main(String[] args) {
        SpringApplication.run(MoaApplication.class, args);
	}

}
