package com.fooddelivery.dto.restaurant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/** Description may be omitted or empty; the database column is nullable and imposes only a 500-character limit. */
public record RestaurantCreateRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 500) String description,
        @NotBlank @Pattern(regexp = "^[+0-9()\\- ]{7,20}$") String phone,
        @NotBlank @Size(max = 190) String addressLine,
        @NotBlank @Size(max = 80) String city,
        @Positive Long locationId,
        @NotBlank @Size(max = 80) String cuisine,
        LocalTime openingTime,
        LocalTime closingTime) { }
