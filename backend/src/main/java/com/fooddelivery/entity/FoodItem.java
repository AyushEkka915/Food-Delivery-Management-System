package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "food_items")
public class FoodItem extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "restaurant_id", nullable = false) private Restaurant restaurant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "category_id", nullable = false) private FoodCategory category;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 500) private String description;
    @Column(nullable = false, precision = 10, scale = 2) private BigDecimal price;
    @Column(name = "is_veg", nullable = false) private boolean vegetarian;
    @Column(name = "is_available", nullable = false) private boolean available;
    @Column(name = "stock_qty", nullable = false) private Integer stockQuantity;
    @Column(name = "prep_time_min", nullable = false) private Integer preparationTimeMinutes;
    @Column(name = "image_url", length = 500) private String imageUrl;
    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2) private BigDecimal ratingAverage;
    @Column(name = "rating_count", nullable = false) private Integer ratingCount;
    protected FoodItem() { }

    public static FoodItem create(Restaurant restaurant, FoodCategory category, String name, String description,
            BigDecimal price, Boolean vegetarian, int stockQuantity, int preparationTimeMinutes, String imageUrl) {
        FoodItem item = new FoodItem();
        item.restaurant = Objects.requireNonNull(restaurant, "restaurant is required");
        item.category = Objects.requireNonNull(category, "category is required");
        item.updateProfile(name, description, price, vegetarian, preparationTimeMinutes, imageUrl);
        item.updateStockQuantity(stockQuantity);
        item.available = true;
        item.ratingAverage = BigDecimal.ZERO.setScale(2);
        item.ratingCount = 0;
        return item;
    }

    public void updateProfile(String name, String description, BigDecimal price, Boolean vegetarian,
            int preparationTimeMinutes, String imageUrl) {
        this.name = requireText(name, "name", 150);
        this.description = validateOptionalText(description, "description", 500);
        this.price = validatePrice(price);
        this.vegetarian = Objects.requireNonNull(vegetarian, "vegetarian is required");
        if (preparationTimeMinutes <= 0) {
            throw new IllegalArgumentException("preparationTimeMinutes must be positive");
        }
        this.preparationTimeMinutes = preparationTimeMinutes;
        this.imageUrl = validateOptionalText(imageUrl, "imageUrl", 500);
    }

    public void changeAvailability(boolean available) { this.available = available; }

    public void changeCategory(FoodCategory category) {
        this.category = Objects.requireNonNull(category, "category is required");
    }

    public void updateStockQuantity(int stockQuantity) {
        if (stockQuantity < 0) throw new IllegalArgumentException("stockQuantity must be nonnegative");
        this.stockQuantity = stockQuantity;
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        if (value.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return value;
    }

    private static String validateOptionalText(String value, String field, int maxLength) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        }
        return value;
    }

    private static BigDecimal validatePrice(BigDecimal price) {
        Objects.requireNonNull(price, "price is required");
        if (price.signum() < 0) throw new IllegalArgumentException("price must be nonnegative");
        BigDecimal normalized = price.stripTrailingZeros();
        if (normalized.scale() > 2 || normalized.precision() - normalized.scale() > 8) {
            throw new IllegalArgumentException("price must fit DECIMAL(10,2)");
        }
        return price;
    }

    public Long getId() { return super.getId(); }
    public Restaurant getRestaurant() { return restaurant; }
    public FoodCategory getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public boolean isVegetarian() { return vegetarian; }
    public boolean isAvailable() { return available; }
    public Integer getStockQuantity() { return stockQuantity; }
    public Integer getPreparationTimeMinutes() { return preparationTimeMinutes; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getRatingAverage() { return ratingAverage; }
    public Integer getRatingCount() { return ratingCount; }
}
