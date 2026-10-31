package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "coupons")
@Check(constraints = "type in ('PERCENT', 'FLAT') and value >= 0 and valid_to > valid_from")
public class Coupon extends AuditableEntity {
    @Column(name = "code", nullable = false, unique = true, length = 32)
    private String code;

    @Column(name = "type", nullable = false, length = 16)
    private String type;

    @Column(name = "value", nullable = false, precision = 10, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount = new BigDecimal("0.00");

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maximumDiscount;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDateTime validTo;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", nullable = false)
    private Integer usedCount = 0;

    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "first_order_only", nullable = false)
    private boolean firstOrderOnly;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected Coupon() { }

    public static Coupon create(String code, String type, BigDecimal value, BigDecimal minimumOrderAmount,
            BigDecimal maximumDiscount, LocalDateTime validFrom, LocalDateTime validTo,
            Integer usageLimit, Integer perUserLimit, boolean firstOrderOnly) {
        Coupon coupon = new Coupon();
        coupon.code = requireText(code, "code", 32);
        coupon.type = Objects.requireNonNull(type, "type is required");
        if (!type.equals("PERCENT") && !type.equals("FLAT")) {
            throw new IllegalArgumentException("type must be PERCENT or FLAT");
        }
        coupon.value = requireMoney(value, "value", true);
        coupon.minimumOrderAmount = minimumOrderAmount == null
                ? new BigDecimal("0.00") : requireMoney(minimumOrderAmount, "minimumOrderAmount", false);
        coupon.maximumDiscount = maximumDiscount == null ? null : requireMoney(maximumDiscount, "maximumDiscount", false);
        coupon.validFrom = Objects.requireNonNull(validFrom, "validFrom is required");
        coupon.validTo = Objects.requireNonNull(validTo, "validTo is required");
        if (!validTo.isAfter(validFrom)) throw new IllegalArgumentException("validTo must be after validFrom");
        coupon.usageLimit = usageLimit;
        coupon.usedCount = 0;
        coupon.perUserLimit = perUserLimit;
        coupon.firstOrderOnly = firstOrderOnly;
        coupon.active = true;
        return coupon;
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        if (value.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return value;
    }

    private static BigDecimal requireMoney(BigDecimal amount, String field, boolean strictlyPositive) {
        Objects.requireNonNull(amount, field + " is required");
        if (strictlyPositive ? amount.signum() <= 0 : amount.signum() < 0) {
            throw new IllegalArgumentException(field + (strictlyPositive ? " must be positive" : " must be nonnegative"));
        }
        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 2 || normalized.precision() - normalized.scale() > 8) {
            throw new IllegalArgumentException(field + " must fit DECIMAL(10,2)");
        }
        return amount;
    }

    public String getCode() { return code; }
    public String getType() { return type; }
    public BigDecimal getValue() { return value; }
    public BigDecimal getMinimumOrderAmount() { return minimumOrderAmount; }
    public BigDecimal getMaximumDiscount() { return maximumDiscount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public Integer getUsageLimit() { return usageLimit; }
    public Integer getUsedCount() { return usedCount; }
    public Integer getPerUserLimit() { return perUserLimit; }
    public boolean isFirstOrderOnly() { return firstOrderOnly; }
    public boolean isActive() { return active; }
}
