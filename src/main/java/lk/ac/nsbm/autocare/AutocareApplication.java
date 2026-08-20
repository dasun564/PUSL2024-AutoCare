package lk.ac.nsbm.autocare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AutoCare - vehicle service management system for a motor garage.
 *
 * PUSL2024 Software Engineering 2, Referral Coursework 2025-2026, element C1.
 * Student: Dasun Edirisinghe (10965261).
 *
 * Customers register their vehicles and book services; garage staff plan the
 * work by adding parts and labour to a job, then close it, which consumes the
 * parts from stock and raises the invoice in a single atomic operation.
 */
@SpringBootApplication
public class AutocareApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutocareApplication.class, args);
    }
}
