package com.fooddelivery.dto.restaurant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Locale;

/** Query parameters for the public restaurant browse endpoint. */
public record RestaurantCatalogQuery(
        @Size(max = 150, message = "name search must be at most 150 characters") String name,
        @Size(max = 80, message = "cuisine must be at most 80 characters") String cuisine,
        @DecimalMin(value = "0.00", message = "minRating must be at least 0")
        @DecimalMax(value = "5.00", message = "minRating must be at most 5")
        @Digits(integer = 1, fraction = 2, message = "minRating must have at most 2 decimal places")
        BigDecimal minRating,
        @DecimalMin(value = "0.00", message = "maxRating must be at least 0")
        @DecimalMax(value = "5.00", message = "maxRating must be at most 5")
        @Digits(integer = 1, fraction = 2, message = "maxRating must have at most 2 decimal places")
        BigDecimal maxRating,
        Boolean vegetarian,
        @DecimalMin(value = "0.00", message = "minPrice must not be negative")
        @Digits(integer = 8, fraction = 2, message = "minPrice must fit DECIMAL(10,2)")
        BigDecimal minPrice,
        @DecimalMin(value = "0.00", message = "maxPrice must not be negative")
        @Digits(integer = 8, fraction = 2, message = "maxPrice must fit DECIMAL(10,2)")
        BigDecimal maxPrice,
        @NotNull @Min(value = 0, message = "page must be nonnegative") Integer page,
        @NotNull @Min(value = 1, message = "size must be at least 1")
        @Max(value = 100, message = "size must not exceed 100") Integer size,
        @NotNull @Pattern(regexp = "name|cuisine|ratingAverage|ratingCount",
                message = "sortBy must be one of: name, cuisine, ratingAverage, ratingCount") String sortBy,
        @NotNull @Pattern(regexp = "ASC|DESC", message = "sortDirection must be ASC or DESC") String sortDirection) {

    public RestaurantCatalogQuery {
        name = normalizeFilter(name);
        cuisine = normalizeFilter(cuisine);
        page = page == null ? 0 : page;
        size = size == null ? 20 : size;
        sortBy = sortBy == null || sortBy.isBlank() ? "name" : sortBy.trim();
        sortDirection = sortDirection == null || sortDirection.isBlank()
                ? "ASC"
                : sortDirection.trim().toUpperCase(Locale.ROOT);
    }

    @AssertTrue(message = "minRating must not exceed maxRating")
    public boolean isRatingRangeValid() {
        return minRating == null || maxRating == null || minRating.compareTo(maxRating) <= 0;
    }

    @AssertTrue(message = "minPrice must not exceed maxPrice")
    public boolean isPriceRangeValid() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    private static String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
