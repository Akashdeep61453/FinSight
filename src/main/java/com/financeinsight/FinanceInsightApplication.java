package com.financeinsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinanceInsightApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceInsightApplication.class, args);
    }
}
