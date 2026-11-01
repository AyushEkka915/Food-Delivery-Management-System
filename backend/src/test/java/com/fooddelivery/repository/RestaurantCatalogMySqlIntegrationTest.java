package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fooddelivery.dto.restaurant.RestaurantCatalogQuery;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.enums.RestaurantStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

/**
 * MySQL-only repository integration coverage. Run explicitly with the test profile;
 * the test infrastructure guard validates the datasource before any database bean starts.
 * Each method runs in a rollback-only test transaction, so all fixtures are removed on completion.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Rollback
class RestaurantCatalogMySqlIntegrationTest {
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private FoodItemRepository foodItemRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String testKey;
    private String cuisine;
    private int ownerSequence;
    private int phoneSequence;
    private int itemSequence;
    private Long categoryId;

    @BeforeEach
    void initializeUniqueFixtureKey(TestInfo testInfo) {
        String methodName = testInfo.getTestMethod().orElseThrow().getName();
        String readablePart = methodName.substring(0, Math.min(methodName.length(), 36));
        String stableSuffix = Integer.toUnsignedString(methodName.hashCode(), 36);
        testKey = "catalog_it_" + readablePart + "_" + stableSuffix;
        cuisine = testKey;
        ownerSequence = 1;
        phoneSequence = 1;
        itemSequence = 1;
        categoryId = null;
    }

    @Test
    void listingExposesApprovedRestaurantsOnly() {
        TestRestaurant approved = restaurant("Approved " + testKey, RestaurantStatus.APPROVED, "3.00", 1);
        restaurant("Pending " + testKey, RestaurantStatus.PENDING, "3.00", 1);
        restaurant("Suspended " + testKey, RestaurantStatus.SUSPENDED, "3.00", 1);

        Page<Restaurant> result = search(null, cuisine, null, null, null, null, null, 0, 20, "name", "ASC");

        assertEquals(List.of(approved.id()), ids(result));
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void nameSearchIsCaseInsensitivePartialAndEscapesLikeMetacharacters() {
        TestRestaurant mixedCase = restaurant("MiXeD Menu " + testKey, RestaurantStatus.APPROVED, "4.00", 2);
        TestRestaurant percent = restaurant("100% Bowl " + testKey, RestaurantStatus.APPROVED, "4.00", 2);
        restaurant("100X Bowl " + testKey, RestaurantStatus.APPROVED, "4.00", 2);
        TestRestaurant underscore = restaurant("A_B Plate " + testKey, RestaurantStatus.APPROVED, "4.00", 2);
        restaurant("AXB Plate " + testKey, RestaurantStatus.APPROVED, "4.00", 2);
        TestRestaurant backslash = restaurant("Path\\Meal " + testKey, RestaurantStatus.APPROVED, "4.00", 2);
        restaurant("PathXMeal " + testKey, RestaurantStatus.APPROVED, "4.00", 2);

        assertEquals(List.of(mixedCase.id()), ids(search("mixed menu", cuisine, null, null,
                null, null, null, 0, 20, "name", "ASC")));
        assertEquals(List.of(percent.id()), ids(search("100%", cuisine, null, null,
                null, null, null, 0, 20, "name", "ASC")));
        assertEquals(List.of(underscore.id()), ids(search("A_B", cuisine, null, null,
                null, null, null, 0, 20, "name", "ASC")));
        assertEquals(List.of(backslash.id()), ids(search("Path\\Meal", cuisine, null, null,
                null, null, null, 0, 20, "name", "ASC")));
    }

    @Test
    void cuisineAndRatingBoundsUseInclusiveComparisons() {
        TestRestaurant lower = restaurant("Rating lower " + testKey, RestaurantStatus.APPROVED, "3.50", 4);
        TestRestaurant boundary = restaurant("Rating boundary " + testKey, RestaurantStatus.APPROVED, "4.00", 5);
        TestRestaurant upper = restaurant("Rating upper " + testKey, RestaurantStatus.APPROVED, "4.50", 6);

        assertEquals(Set.of(boundary.id(), upper.id()), new HashSet<>(ids(search(null, cuisine.toUpperCase(),
                new BigDecimal("4.00"), null, null, null, null, 0, 20, "name", "ASC"))));
        assertEquals(Set.of(lower.id(), boundary.id()), new HashSet<>(ids(search(null, cuisine,
                null, new BigDecimal("4.00"), null, null, null, 0, 20, "name", "ASC"))));
        assertEquals(Set.of(boundary.id()), new HashSet<>(ids(search(null, cuisine,
                new BigDecimal("4.00"), new BigDecimal("4.00"), null, null, null, 0, 20, "name", "ASC"))));
    }

    @Test
    void vegetarianFilterRequiresAnAvailableInStockVegetarianItem() {
        TestRestaurant qualifying = restaurant("Veg qualifies " + testKey, RestaurantStatus.APPROVED, "4.00", 1);
        item(qualifying.id(), "Available veg", "8.00", true, true, 2);

        TestRestaurant unavailableOnly = restaurant("Veg unavailable " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(unavailableOnly.id(), "Unavailable veg", "8.00", true, false, 4);

        TestRestaurant outOfStockOnly = restaurant("Veg empty " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(outOfStockOnly.id(), "Out of stock veg", "8.00", true, true, 0);

        TestRestaurant nonVegetarian = restaurant("Non veg " + testKey, RestaurantStatus.APPROVED, "4.00", 1);
        item(nonVegetarian.id(), "Available non-veg", "8.00", false, true, 3);

        Page<Restaurant> result = search(null, cuisine, null, null, true,
                null, null, 0, 20, "name", "ASC");

        assertEquals(List.of(qualifying.id()), ids(result));
    }

    @Test
    void minimumPriceBoundaryIsInclusiveAndBelowMinimumIsExcluded() {
        TestRestaurant atMinimum = restaurant("At minimum " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(atMinimum.id(), "Minimum boundary", "10.00", false, true, 1);

        TestRestaurant belowMinimum = restaurant("Below minimum " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(belowMinimum.id(), "Below minimum", "9.99", false, true, 1);

        TestRestaurant withinRange = restaurant("Within range " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(withinRange.id(), "Inside range", "15.00", false, true, 1);

        TestRestaurant unavailable = restaurant("Unavailable range " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(unavailable.id(), "Unavailable in range", "15.00", false, false, 1);

        TestRestaurant outOfStock = restaurant("Out of stock range " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(outOfStock.id(), "Empty in range", "16.00", false, true, 0);

        Page<Restaurant> minBounded = search(null, cuisine, null, null, null,
                new BigDecimal("10.00"), null, 0, 20, "name", "ASC");
        assertEquals(Set.of(atMinimum.id(), withinRange.id()), new HashSet<>(ids(minBounded)),
                "minimum boundary is inclusive and below-minimum item is excluded");
        assertEquals(2, minBounded.getTotalElements());
        assertFalse(ids(minBounded).contains(unavailable.id()));
        assertFalse(ids(minBounded).contains(outOfStock.id()));
    }

    @Test
    void maximumPriceBoundaryIsInclusiveAndAboveMaximumIsExcluded() {
        TestRestaurant atMaximum = restaurant("At maximum " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(atMaximum.id(), "Maximum boundary", "20.00", false, true, 1);

        TestRestaurant aboveMaximum = restaurant("Above maximum " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(aboveMaximum.id(), "Above maximum", "20.01", false, true, 1);

        TestRestaurant withinRange = restaurant("Within range " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(withinRange.id(), "Inside range", "15.00", false, true, 1);

        TestRestaurant unavailable = restaurant("Unavailable range " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(unavailable.id(), "Unavailable in range", "15.00", false, false, 1);

        TestRestaurant outOfStock = restaurant("Out of stock range " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(outOfStock.id(), "Empty in range", "16.00", false, true, 0);

        Page<Restaurant> maxBounded = search(null, cuisine, null, null, null,
                null, new BigDecimal("20.00"), 0, 20, "name", "ASC");
        assertEquals(Set.of(atMaximum.id(), withinRange.id()), new HashSet<>(ids(maxBounded)),
                "maximum boundary is inclusive and above-maximum item is excluded");
        assertEquals(2, maxBounded.getTotalElements());
        assertFalse(ids(maxBounded).contains(unavailable.id()));
        assertFalse(ids(maxBounded).contains(outOfStock.id()));
    }

    @Test
    void separateVegetarianAndPriceExistsPredicatesMayMatchDifferentItemsWithoutDuplicates() {
        TestRestaurant separateItems = restaurant("Separate items " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(separateItems.id(), "Vegetarian outside price", "100.00", true, true, 2);
        item(separateItems.id(), "Non-veg inside price", "5.00", false, true, 2);
        item(separateItems.id(), "Another matching non-veg", "5.50", false, true, 2);

        TestRestaurant noVegetarian = restaurant("No vegetarian " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(noVegetarian.id(), "Only in-range non-veg", "5.00", false, true, 1);

        TestRestaurant noPriceMatch = restaurant("No price match " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        item(noPriceMatch.id(), "Only veg outside range", "100.00", true, true, 1);

        Page<Restaurant> result = search(null, cuisine, null, null, true,
                new BigDecimal("4.00"), new BigDecimal("6.00"), 0, 20, "name", "ASC");

        assertEquals(List.of(separateItems.id()), ids(result));
        assertEquals(1, result.getTotalElements(), "multiple matching items must not duplicate the restaurant");
    }

    @Test
    void paginationAndEverySupportedSortAreStableAcrossPages() {
        List<TestRestaurant> fixtures = List.of(
                restaurant("Sort B " + testKey, "Cuisine Beta " + testKey,
                        RestaurantStatus.APPROVED, "4.00", 2),
                restaurant("Sort A " + testKey, "Cuisine Alpha " + testKey,
                        RestaurantStatus.APPROVED, "4.50", 2),
                restaurant("Sort A " + testKey, "Cuisine Alpha " + testKey,
                        RestaurantStatus.APPROVED, "4.00", 3),
                restaurant("Sort C " + testKey, "Cuisine Gamma " + testKey,
                        RestaurantStatus.APPROVED, "4.00", 3),
                restaurant("Sort B " + testKey, "Cuisine Beta " + testKey,
                        RestaurantStatus.APPROVED, "3.50", 1));

        for (String sortBy : List.of("name", "cuisine", "ratingAverage", "ratingCount")) {
            for (String direction : List.of("ASC", "DESC")) {
                Comparator<TestRestaurant> comparator = comparator(sortBy, direction);
                List<Long> expected = fixtures.stream().sorted(comparator)
                        .map(TestRestaurant::id).toList();
                List<Long> actual = new ArrayList<>();
                for (int page = 0; page < 3; page++) {
                    Page<Restaurant> result = search(testKey, null, null, null, null,
                            null, null, page, 2, sortBy, direction);
                    actual.addAll(ids(result));
                    assertEquals(fixtures.size(), result.getTotalElements());
                }
                assertEquals(expected, actual, sortBy + " " + direction);
                assertEquals(actual.size(), new HashSet<>(actual).size(), "pages must not duplicate restaurants");
            }
        }
    }

    @Test
    void approvedDetailAndPublicMenuHideUnapprovedAndUnorderableRecords() {
        TestRestaurant approved = restaurant("Public approved " + testKey,
                RestaurantStatus.APPROVED, "4.00", 1);
        TestRestaurant pending = restaurant("Public pending " + testKey,
                RestaurantStatus.PENDING, "4.00", 1);
        TestRestaurant suspended = restaurant("Public suspended " + testKey,
                RestaurantStatus.SUSPENDED, "4.00", 1);

        Long visibleItemId = item(approved.id(), "Visible item A", "12.00", true, true, 4);
        Long secondVisibleItemId = item(approved.id(), "Visible item B", "12.50", false, true, 3);
        item(approved.id(), "Unavailable item", "13.00", true, false, 5);
        item(approved.id(), "Out of stock item", "14.00", true, true, 0);
        item(pending.id(), "Pending item", "15.00", true, true, 2);
        item(suspended.id(), "Suspended item", "16.00", true, true, 2);

        assertTrue(restaurantRepository.findApprovedById(approved.id()).isPresent());
        assertTrue(restaurantRepository.findApprovedById(pending.id()).isEmpty());
        assertTrue(restaurantRepository.findApprovedById(suspended.id()).isEmpty());

        Page<FoodItem> firstMenuPage = foodItemRepository
                .findPublicOrderableMenuByRestaurantId(approved.id(), PageRequest.of(0, 1));
        assertEquals(2, firstMenuPage.getTotalElements());
        assertEquals(List.of(visibleItemId), firstMenuPage.getContent().stream()
                .map(FoodItem::getId).toList());
        assertTrue(org.hibernate.Hibernate.isInitialized(firstMenuPage.getContent().get(0).getCategory()),
                "category should be fetched by the public menu query");
        assertEquals("Catalog " + testKey.substring(testKey.lastIndexOf('_') + 1),
                firstMenuPage.getContent().get(0).getCategory().getName());

        Page<FoodItem> secondMenuPage = foodItemRepository
                .findPublicOrderableMenuByRestaurantId(approved.id(), PageRequest.of(1, 1));
        assertEquals(2, secondMenuPage.getTotalElements(), "count should cover all filtered menu items");
        assertEquals(List.of(secondVisibleItemId), secondMenuPage.getContent().stream()
                .map(FoodItem::getId).toList());

        Page<FoodItem> pendingMenu = foodItemRepository
                .findPublicOrderableMenuByRestaurantId(pending.id(), PageRequest.of(0, 20));
        Page<FoodItem> suspendedMenu = foodItemRepository
                .findPublicOrderableMenuByRestaurantId(suspended.id(), PageRequest.of(0, 20));
        assertTrue(pendingMenu.isEmpty());
        assertEquals(0, pendingMenu.getTotalElements());
        assertTrue(suspendedMenu.isEmpty());
        assertEquals(0, suspendedMenu.getTotalElements());
    }

    private Page<Restaurant> search(String name, String cuisineFilter,
            BigDecimal minRating, BigDecimal maxRating, Boolean vegetarian,
            BigDecimal minPrice, BigDecimal maxPrice, int page, int size,
            String sortBy, String direction) {
        RestaurantCatalogQuery query = new RestaurantCatalogQuery(name, cuisineFilter,
                minRating, maxRating, vegetarian, minPrice, maxPrice,
                page, size, sortBy, direction);
        return restaurantRepository.searchApproved(query);
    }

    private TestRestaurant restaurant(String name, RestaurantStatus status, String ratingAverage, int ratingCount) {
        return restaurant(name, cuisine, status, ratingAverage, ratingCount);
    }

    private TestRestaurant restaurant(String name, String restaurantCuisine, RestaurantStatus status,
            String ratingAverage, int ratingCount) {
        Long ownerId = insertOwner();
        String phone = String.format("+998%09d", phoneSequence++);
        BigDecimal rating = new BigDecimal(ratingAverage);
        jdbcTemplate.update("""
                INSERT INTO restaurants (
                    owner_user_id, name, description, phone, address_line, city, location_id,
                    cuisine, status, commission_rate, rating_avg, rating_count
                ) VALUES (?, ?, NULL, ?, ?, ?, NULL, ?, ?, 12.00, ?, ?)
                """, ownerId, name, phone, "Test address", "Test city", restaurantCuisine,
                status.name(), rating, ratingCount);
        Long restaurantId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new TestRestaurant(restaurantId, name, restaurantCuisine, rating, ratingCount);
    }

    private Long insertOwner() {
        String email = "catalog-it-" + testKey + "-" + ownerSequence + "@example.test";
        String phone = String.format("+997%09d", phoneSequence++);
        ownerSequence++;
        jdbcTemplate.update("""
                INSERT INTO users (name, email, phone, password_hash, status)
                VALUES (?, ?, ?, ?, 'ACTIVE')
                """, "Catalog Test Owner", email, phone, "test-only-not-a-credential");
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long item(Long restaurantId, String label, String price, boolean vegetarian,
            boolean available, int stockQuantity) {
        if (categoryId == null) {
            String categoryName = "Catalog " + testKey.substring(testKey.lastIndexOf('_') + 1);
            jdbcTemplate.update("INSERT INTO food_categories (name, display_order) VALUES (?, 0)", categoryName);
            categoryId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        }
        String itemName = label + " " + testKey + " " + itemSequence++;
        jdbcTemplate.update("""
                INSERT INTO food_items (
                    restaurant_id, category_id, name, description, price, is_veg,
                    is_available, stock_qty, prep_time_min, rating_avg, rating_count
                ) VALUES (?, ?, ?, NULL, ?, ?, ?, ?, 15, 0.00, 0)
                """, restaurantId, categoryId, itemName, new BigDecimal(price),
                vegetarian, available, stockQuantity);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private static List<Long> ids(Page<Restaurant> page) {
        return page.getContent().stream().map(Restaurant::getId).toList();
    }

    private static Comparator<TestRestaurant> comparator(String sortBy, String direction) {
        Comparator<TestRestaurant> primary = switch (sortBy) {
            case "name" -> Comparator.comparing(TestRestaurant::name);
            case "cuisine" -> Comparator.comparing(TestRestaurant::cuisine);
            case "ratingAverage" -> Comparator.comparing(TestRestaurant::ratingAverage);
            case "ratingCount" -> Comparator.comparingInt(TestRestaurant::ratingCount);
            default -> throw new IllegalArgumentException("Unsupported test sort field: " + sortBy);
        };
        if ("DESC".equals(direction)) {
            primary = primary.reversed();
        }
        return primary.thenComparing(TestRestaurant::id);
    }

    private record TestRestaurant(Long id, String name, String cuisine,
                                  BigDecimal ratingAverage, int ratingCount) { }
}
