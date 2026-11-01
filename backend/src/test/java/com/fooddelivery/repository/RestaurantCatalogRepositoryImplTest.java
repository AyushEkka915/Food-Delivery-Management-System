package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fooddelivery.dto.restaurant.RestaurantCatalogQuery;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

class RestaurantCatalogRepositoryImplTest {
    @Test
    void defaultSortIsNameAscendingWithStableIdTieBreaker() {
        Sort sort = RestaurantCatalogRepositoryImpl.stableSort(query("name", "ASC"));

        assertEquals(Sort.by(Sort.Order.asc("name"), Sort.Order.asc("id")), sort);
    }

    @Test
    void allowedRatingSortHonorsDirectionAndAddsStableIdTieBreaker() {
        Sort sort = RestaurantCatalogRepositoryImpl.stableSort(query("ratingAverage", "DESC"));

        assertEquals(Sort.by(Sort.Order.desc("ratingAverage"), Sort.Order.asc("id")), sort);
    }

    @Test
    void unsupportedSortPropertiesAreRejectedInsteadOfUsedAsQueryPaths() {
        assertThrows(IllegalArgumentException.class,
                () -> RestaurantCatalogRepositoryImpl.stableSort(query("owner.email", "ASC")));
    }

    @Test
    void unsupportedSortDirectionsAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> RestaurantCatalogRepositoryImpl.stableSort(query("name", "SIDEWAYS")));
    }

    @Test
    void approvedDetailLookupPinsTheVisibilityPredicateAndLoadsLocation() throws Exception {
        var method = RestaurantRepository.class.getMethod("findApprovedById", Long.class);
        Query query = method.getAnnotation(Query.class);
        EntityGraph entityGraph = method.getAnnotation(EntityGraph.class);

        assertTrue(query.value().contains("RestaurantStatus.APPROVED"));
        assertEquals("location", entityGraph.attributePaths()[0]);
    }

    @Test
    void publicMenuContentAndCountQueriesRequireApprovedAvailableInStockItems() throws Exception {
        var method = FoodItemRepository.class.getMethod("findPublicOrderableMenuByRestaurantId",
                Long.class, org.springframework.data.domain.Pageable.class);
        Query query = method.getAnnotation(Query.class);

        for (String requiredPredicate : new String[] {
                "RestaurantStatus.APPROVED", "item.available = true", "item.stockQuantity > 0"}) {
            assertTrue(query.value().contains(requiredPredicate), "content query missing " + requiredPredicate);
            assertTrue(query.countQuery().contains(requiredPredicate), "count query missing " + requiredPredicate);
        }
        assertTrue(query.value().contains("join fetch item.category"));
        assertTrue(query.value().contains("order by item.name asc, item.id asc"));
    }

    private static RestaurantCatalogQuery query(String sortBy, String sortDirection) {
        return new RestaurantCatalogQuery(null, null, null, null, null, null, null,
                0, 20, sortBy, sortDirection);
    }
}
