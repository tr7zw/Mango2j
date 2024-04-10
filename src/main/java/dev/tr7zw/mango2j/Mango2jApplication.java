package dev.tr7zw.mango2j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class Mango2jApplication {

	public static void main(String[] args) {
		SpringApplication.run(Mango2jApplication.class, args);
	}

}
