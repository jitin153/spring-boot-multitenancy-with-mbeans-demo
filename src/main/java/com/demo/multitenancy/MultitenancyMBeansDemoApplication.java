package com.demo.multitenancy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.EnableMBeanExport;

@SpringBootApplication(exclude = { HibernateJpaAutoConfiguration.class })
@EnableMBeanExport
public class MultitenancyMBeansDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(MultitenancyMBeansDemoApplication.class, args);
	}
}
