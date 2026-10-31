package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "order_items")
@Check(constraints = "quantity > 0 and unit_price_snapshot >= 0 and line_total >= 0")
public class OrderItem {
    @jakarta.persistence.Id
    @jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_item_id")
    private FoodItem foodItem;

    @Column(name = "item_name_snapshot", nullable = false, length = 150)
    private String itemNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal lineTotal;

    @Column(name = "notes", length = 255)
    private String notes;

    protected OrderItem() { }

    public static OrderItem create(Order order, FoodItem foodItem, String itemNameSnapshot,
            BigDecimal unitPriceSnapshot, int quantity, BigDecimal lineTotal, String notes) {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        OrderItem item = new OrderItem();
        item.order = Objects.requireNonNull(order, "order is required");
        item.foodItem = foodItem;
        item.itemNameSnapshot = requireText(itemNameSnapshot, "itemNameSnapshot", 150);
        item.unitPriceSnapshot = requireMoney(unitPriceSnapshot, "unitPriceSnapshot");
        item.quantity = quantity;
        item.lineTotal = requireMoney(lineTotal, "lineTotal");
        if (notes != null && notes.length() > 255) throw new IllegalArgumentException("notes exceeds 255 characters");
        item.notes = notes;
        return item;
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " is required");
        if (value.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return value;
    }

    private static BigDecimal requireMoney(BigDecimal amount, String field) {
        Objects.requireNonNull(amount, field + " is required");
        if (amount.signum() < 0) throw new IllegalArgumentException(field + " must be nonnegative");
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 2 || normalized.precision() - normalized.scale() > 8) {
            throw new IllegalArgumentException(field + " must fit DECIMAL(10,2)");
        }
        return amount;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public FoodItem getFoodItem() { return foodItem; }
    public String getItemNameSnapshot() { return itemNameSnapshot; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public String getNotes() { return notes; }
}
