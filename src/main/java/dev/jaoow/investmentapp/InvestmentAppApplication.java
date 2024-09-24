package dev.jaoow.investmentapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class InvestmentAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestmentAppApplication.class, args);
    }

}
