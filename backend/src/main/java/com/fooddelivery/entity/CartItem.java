package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "cart_items", uniqueConstraints = @UniqueConstraint(
        name = "uk_cart_items_user_item", columnNames = {"user_id", "food_item_id"}))
@Check(constraints = "quantity > 0 and quantity <= 20")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_item_id", nullable = false)
    private FoodItem foodItem;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "added_at", nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime addedAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;

    protected CartItem() { }

    public static CartItem create(User user, FoodItem foodItem, int quantity) {
        CartItem item = new CartItem();
        item.user = Objects.requireNonNull(user, "user is required");
        item.foodItem = Objects.requireNonNull(foodItem, "foodItem is required");
        item.setQuantity(quantity);
        return item;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        addedAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void setQuantity(int quantity) {
        if (quantity < 1 || quantity > 20) {
            throw new IllegalArgumentException("quantity must be between 1 and 20");
        }
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public FoodItem getFoodItem() { return foodItem; }
    public Integer getQuantity() { return quantity; }
    public LocalDateTime getAddedAt() { return addedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
