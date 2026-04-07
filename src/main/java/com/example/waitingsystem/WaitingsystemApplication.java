package com.example.waitingsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
@MapperScan("com.example.waitingsystem.repository")
public class WaitingsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(WaitingsystemApplication.class, args);
	}

}
