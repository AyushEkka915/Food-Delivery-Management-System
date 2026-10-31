package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "reviews", uniqueConstraints =
        @UniqueConstraint(name = "uk_reviews_order", columnNames = "order_id"))
@Check(constraints = "rating between 1 and 5")
public class Review extends CreatedOnlyEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(name = "rating", nullable = false)
    private Byte rating;

    @Column(name = "comment", length = 1000)
    private String comment;

    protected Review() { }

    public static Review create(Order order, User customer, Restaurant restaurant, byte rating, String comment) {
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("rating must be between 1 and 5");
        Review review = new Review();
        review.order = Objects.requireNonNull(order, "order is required");
        review.customer = Objects.requireNonNull(customer, "customer is required");
        review.restaurant = Objects.requireNonNull(restaurant, "restaurant is required");
        review.rating = rating;
        review.comment = comment;
        return review;
    }

    public Order getOrder() { return order; }
    public User getCustomer() { return customer; }
    public Restaurant getRestaurant() { return restaurant; }
    public Byte getRating() { return rating; }
    public String getComment() { return comment; }
}
