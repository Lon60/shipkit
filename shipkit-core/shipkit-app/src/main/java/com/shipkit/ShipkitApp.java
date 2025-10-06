package com.shipkit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan("com.shipkit.api")
@EnableJpaRepositories("com.shipkit.api")
@SpringBootApplication(scanBasePackages = "com.shipkit")
public class ShipkitApp {

    static void main(String[] args) {
        SpringApplication.run(ShipkitApp.class, args);
    }

}
