package com.parkhyuns00.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class BlogApplication {

	static void main(String[] args) {
		SpringApplication.run(BlogApplication.class, args);
	}
}
