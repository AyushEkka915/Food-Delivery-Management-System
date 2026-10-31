package com.fooddelivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Smart Food Delivery Management System backend.
 *
 * <p>Layered architecture: controller -> service -> repository -> entity.
 * The custom DSA implementations live in {@code com.fooddelivery.dsa} and have
 * no dependency on Spring or JPA, so they can be unit-tested in isolation.
 */
@SpringBootApplication
public class FoodDeliveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoodDeliveryApplication.class, args);
    }
}
