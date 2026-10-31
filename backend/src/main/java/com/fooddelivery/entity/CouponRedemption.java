package com.fooddelivery.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(name = "coupon_redemptions", uniqueConstraints = @UniqueConstraint(
        name = "uk_coupon_redemptions_coupon_order", columnNames = {"coupon_id", "order_id"}))
public class CouponRedemption extends CreatedOnlyEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    protected CouponRedemption() { }

    public static CouponRedemption create(Coupon coupon, User user, Order order) {
        CouponRedemption redemption = new CouponRedemption();
        redemption.coupon = Objects.requireNonNull(coupon, "coupon is required");
        redemption.user = Objects.requireNonNull(user, "user is required");
        redemption.order = Objects.requireNonNull(order, "order is required");
        return redemption;
    }

    public Coupon getCoupon() { return coupon; }
    public User getUser() { return user; }
    public Order getOrder() { return order; }
}
