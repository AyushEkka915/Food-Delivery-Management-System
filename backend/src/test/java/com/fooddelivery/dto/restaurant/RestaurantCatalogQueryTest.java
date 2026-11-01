package com.fooddelivery.dto.restaurant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RestaurantCatalogQueryTest {
    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void nullQueryValuesReceiveApprovedDefaultsAndValidate() {
        RestaurantCatalogQuery query = query(null, null, null, null, null, null, null,
                null, null, null, null);

        assertEquals(0, query.page());
        assertEquals(20, query.size());
        assertEquals("name", query.sortBy());
        assertEquals("ASC", query.sortDirection());
        assertTrue(validator.validate(query).isEmpty());
    }

    @Test
    void rejectsNegativePage() {
        assertInvalid(query(null, null, null, null, null, null, null, -1, 20, "name", "ASC"), "page");
    }

    @Test
    void rejectsSizeAboveOneHundred() {
        assertInvalid(query(null, null, null, null, null, null, null, 0, 101, "name", "ASC"), "size");
    }

    @Test
    void rejectsRatingOutsideZeroToFive() {
        assertInvalid(query(null, null, new BigDecimal("5.01"), null, null, null, null,
                0, 20, "name", "ASC"), "minRating");
    }

    @Test
    void rejectsMinimumRatingGreaterThanMaximum() {
        assertInvalid(query(null, null, new BigDecimal("4.5"), new BigDecimal("4.0"), null,
                null, null, 0, 20, "name", "ASC"), "minRating must not exceed maxRating");
    }

    @Test
    void rejectsNegativePrice() {
        assertInvalid(query(null, null, null, null, null, new BigDecimal("-0.01"), null,
                0, 20, "name", "ASC"), "minPrice");
    }

    @Test
    void rejectsMinimumPriceGreaterThanMaximum() {
        assertInvalid(query(null, null, null, null, null, new BigDecimal("20.00"),
                new BigDecimal("10.00"), 0, 20, "name", "ASC"), "minPrice must not exceed maxPrice");
    }

    @Test
    void rejectsPriceOutsideDecimalPrecision() {
        assertInvalid(query(null, null, null, null, null, new BigDecimal("100000000.00"), null,
                0, 20, "name", "ASC"), "minPrice");
    }

    @Test
    void rejectsUnsupportedSortField() {
        assertInvalid(query(null, null, null, null, null, null, null,
                0, 20, "owner.email", "ASC"), "sortBy");
    }

    @Test
    void rejectsInvalidSortDirection() {
        assertInvalid(query(null, null, null, null, null, null, null,
                0, 20, "name", "SIDEWAYS"), "sortDirection");
    }

    private static void assertInvalid(RestaurantCatalogQuery query, String expectedMessagePart) {
        Set<String> messages = validator.validate(query).stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
        assertFalse(messages.stream().noneMatch(message -> message.contains(expectedMessagePart)),
                () -> "Expected validation message containing " + expectedMessagePart + " but got " + messages);
    }

    private static RestaurantCatalogQuery query(String name, String cuisine, BigDecimal minRating,
            BigDecimal maxRating, Boolean vegetarian, BigDecimal minPrice, BigDecimal maxPrice,
            Integer page, Integer size, String sortBy, String sortDirection) {
        return new RestaurantCatalogQuery(name, cuisine, minRating, maxRating, vegetarian,
                minPrice, maxPrice, page, size, sortBy, sortDirection);
    }
}
