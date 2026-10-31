package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class Order extends AuditableEntity {
    @Column(name = "order_number", nullable = false, unique = true, length = 24)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    private Address address;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "address_snapshot", nullable = false, columnDefinition = "JSON")
    private Map<String, Object> addressSnapshot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_location_id")
    private Location deliveryLocation;

    @Column(name = "status", nullable = false, length = 24)
    private String status = "PLACED";

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discount = new BigDecimal("0.00");

    @Column(name = "tax", nullable = false, precision = 10, scale = 2)
    private BigDecimal tax = new BigDecimal("0.00");

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal deliveryFee = new BigDecimal("0.00");

    @Column(name = "total", nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Column(name = "payment_method", nullable = false, length = 16)
    private String paymentMethod;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "prepared_at")
    private LocalDateTime preparedAt;

    @Column(name = "ready_at")
    private LocalDateTime readyAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Order() { }

    public static Order create(String orderNumber, User customer, Restaurant restaurant, Address address,
            Map<String, Object> addressSnapshot, Location deliveryLocation, BigDecimal subtotal, BigDecimal total,
            String paymentMethod) {
        Order order = new Order();
        order.orderNumber = Objects.requireNonNull(orderNumber, "orderNumber is required");
        order.customer = Objects.requireNonNull(customer, "customer is required");
        order.restaurant = Objects.requireNonNull(restaurant, "restaurant is required");
        order.address = address;
        order.addressSnapshot = Objects.requireNonNull(addressSnapshot, "addressSnapshot is required");
        order.deliveryLocation = deliveryLocation;
        order.subtotal = Objects.requireNonNull(subtotal, "subtotal is required");
        order.total = Objects.requireNonNull(total, "total is required");
        order.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod is required");
        order.placedAt = LocalDateTime.now();
        return order;
    }

    public Long getId() { return super.getId(); }
    public Coupon getCoupon() { return coupon; }
    public void applyCoupon(Coupon coupon) { this.coupon = Objects.requireNonNull(coupon, "coupon is required"); }
    public void removeCoupon() { this.coupon = null; }
    public String getOrderNumber() { return orderNumber; }
    public User getCustomer() { return customer; }
    public Restaurant getRestaurant() { return restaurant; }
    public Address getAddress() { return address; }
    public Map<String, Object> getAddressSnapshot() { return addressSnapshot; }
    public Location getDeliveryLocation() { return deliveryLocation; }
    public String getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getDiscount() { return discount; }
    public BigDecimal getTax() { return tax; }
    public BigDecimal getDeliveryFee() { return deliveryFee; }
    public BigDecimal getTotal() { return total; }
    public String getPaymentMethod() { return paymentMethod; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public LocalDateTime getPreparedAt() { return preparedAt; }
    public LocalDateTime getReadyAt() { return readyAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public String getCancelReason() { return cancelReason; }
    public Long getVersion() { return version; }
}
