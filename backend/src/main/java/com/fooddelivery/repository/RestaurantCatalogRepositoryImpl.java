package com.fooddelivery.repository;

import com.fooddelivery.dto.restaurant.RestaurantCatalogQuery;
import com.fooddelivery.entity.FoodItem;
import com.fooddelivery.entity.Restaurant;
import com.fooddelivery.entity.enums.RestaurantStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class RestaurantCatalogRepositoryImpl implements RestaurantCatalogRepository {
    private final EntityManager entityManager;

    public RestaurantCatalogRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Restaurant> searchApproved(RestaurantCatalogQuery query) {
        long offset = (long) query.page() * query.size();
        if (offset > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Requested page offset is too large");
        }

        Sort sort = stableSort(query);
        PageRequest pageable = PageRequest.of(query.page(), query.size(), sort);
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Restaurant> contentCriteria = builder.createQuery(Restaurant.class);
        Root<Restaurant> contentRoot = contentCriteria.from(Restaurant.class);
        contentRoot.fetch("location", JoinType.LEFT);
        contentCriteria.select(contentRoot)
                .where(predicates(builder, contentCriteria, contentRoot, query).toArray(Predicate[]::new))
                .orderBy(criteriaOrders(builder, contentRoot, sort));

        TypedQuery<Restaurant> contentQuery = entityManager.createQuery(contentCriteria);
        contentQuery.setFirstResult((int) offset);
        contentQuery.setMaxResults(query.size());
        List<Restaurant> content = contentQuery.getResultList();

        CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
        Root<Restaurant> countRoot = countCriteria.from(Restaurant.class);
        countCriteria.select(builder.count(countRoot))
                .where(predicates(builder, countCriteria, countRoot, query).toArray(Predicate[]::new));
        long total = entityManager.createQuery(countCriteria).getSingleResult();

        return new PageImpl<>(content, pageable, total);
    }

    static Sort stableSort(RestaurantCatalogQuery query) {
        String property = switch (query.sortBy()) {
            case "name" -> "name";
            case "cuisine" -> "cuisine";
            case "ratingAverage" -> "ratingAverage";
            case "ratingCount" -> "ratingCount";
            default -> throw new IllegalArgumentException("Unsupported restaurant sort field");
        };
        Sort.Direction direction = switch (query.sortDirection()) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default -> throw new IllegalArgumentException("Unsupported sort direction");
        };
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private static List<Predicate> predicates(CriteriaBuilder builder, CriteriaQuery<?> criteria,
                                              Root<Restaurant> restaurant, RestaurantCatalogQuery query) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(restaurant.get("status"), RestaurantStatus.APPROVED));

        if (query.name() != null) {
            String pattern = "%" + escapeLike(query.name().toLowerCase(Locale.ROOT)) + "%";
            predicates.add(builder.like(builder.lower(restaurant.get("name")), pattern, '\\'));
        }
        if (query.cuisine() != null) {
            // The MySQL column collation is case-insensitive; direct equality preserves its index use.
            predicates.add(builder.equal(restaurant.get("cuisine"), query.cuisine()));
        }
        if (query.minRating() != null) {
            predicates.add(builder.greaterThanOrEqualTo(restaurant.<BigDecimal>get("ratingAverage"), query.minRating()));
        }
        if (query.maxRating() != null) {
            predicates.add(builder.lessThanOrEqualTo(restaurant.<BigDecimal>get("ratingAverage"), query.maxRating()));
        }
        if (Boolean.TRUE.equals(query.vegetarian())) {
            predicates.add(orderableItemExists(builder, criteria, restaurant, true, null, null));
        }
        if (query.minPrice() != null || query.maxPrice() != null) {
            predicates.add(orderableItemExists(builder, criteria, restaurant, false,
                    query.minPrice(), query.maxPrice()));
        }
        return predicates;
    }

    private static Predicate orderableItemExists(CriteriaBuilder builder, CriteriaQuery<?> criteria,
                                                  Root<Restaurant> restaurant, boolean vegetarian,
                                                  BigDecimal minPrice, BigDecimal maxPrice) {
        Subquery<Integer> subquery = criteria.subquery(Integer.class);
        Root<FoodItem> item = subquery.from(FoodItem.class);
        List<Predicate> itemPredicates = new ArrayList<>();
        itemPredicates.add(builder.equal(item.get("restaurant").get("id"), restaurant.get("id")));
        itemPredicates.add(builder.isTrue(item.<Boolean>get("available")));
        itemPredicates.add(builder.greaterThan(item.<Integer>get("stockQuantity"), 0));
        if (vegetarian) {
            itemPredicates.add(builder.isTrue(item.<Boolean>get("vegetarian")));
        }
        if (minPrice != null) {
            itemPredicates.add(builder.greaterThanOrEqualTo(item.<BigDecimal>get("price"), minPrice));
        }
        if (maxPrice != null) {
            itemPredicates.add(builder.lessThanOrEqualTo(item.<BigDecimal>get("price"), maxPrice));
        }
        subquery.select(builder.literal(1)).where(itemPredicates.toArray(Predicate[]::new));
        return builder.exists(subquery);
    }

    private static List<Order> criteriaOrders(CriteriaBuilder builder, Root<Restaurant> root, Sort sort) {
        List<Order> orders = new ArrayList<>();
        for (Sort.Order order : sort) {
            orders.add(order.isAscending()
                    ? builder.asc(root.get(order.getProperty()))
                    : builder.desc(root.get(order.getProperty())));
        }
        return orders;
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
