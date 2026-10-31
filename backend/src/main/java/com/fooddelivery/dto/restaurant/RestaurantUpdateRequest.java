package com.fooddelivery.dto.restaurant;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;
import java.util.Optional;

/**
 * Partial restaurant profile update. Omitted fields and explicit JSON nulls are both treated as absent;
 * nullable fields such as description therefore cannot be cleared through this request.
 */
public record RestaurantUpdateRequest(
        Optional<@Size(min = 1, max = 150) String> name,
        Optional<@Size(max = 500) String> description,
        Optional<@Pattern(regexp = "^[+0-9()\\- ]{7,20}$") String> phone,
        Optional<@Size(min = 1, max = 190) String> addressLine,
        Optional<@Size(min = 1, max = 80) String> city,
        Optional<@Positive Long> locationId,
        Optional<@Size(min = 1, max = 80) String> cuisine,
        Optional<LocalTime> openingTime,
        Optional<LocalTime> closingTime) {
    public RestaurantUpdateRequest {
        name = normalize(name);
        description = normalize(description);
        phone = normalize(phone);
        addressLine = normalize(addressLine);
        city = normalize(city);
        locationId = normalize(locationId);
        cuisine = normalize(cuisine);
        openingTime = normalize(openingTime);
        closingTime = normalize(closingTime);
    }

    private static <T> Optional<T> normalize(Optional<T> field) {
        return field == null ? Optional.empty() : field;
    }
}
