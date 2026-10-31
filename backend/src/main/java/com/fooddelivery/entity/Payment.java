package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "payments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_payments_idempotency", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_payments_provider_payment", columnNames = "provider_payment_id")
})
@Check(constraints = "amount >= 0")
public class Payment extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "provider", nullable = false, length = 32)
    private String provider;

    @Column(name = "provider_order_id", length = 128)
    private String providerOrderId;

    @Column(name = "provider_payment_id", length = 128)
    private String providerPaymentId;

    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "method", nullable = false, length = 16)
    private String method;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    protected Payment() { }

    public static Payment create(Order order, String provider, String idempotencyKey, BigDecimal amount, String method) {
        Payment payment = new Payment();
        payment.order = Objects.requireNonNull(order, "order is required");
        payment.provider = Objects.requireNonNull(provider, "provider is required");
        payment.idempotencyKey = Objects.requireNonNull(idempotencyKey, "idempotencyKey is required");
        payment.amount = Objects.requireNonNull(amount, "amount is required");
        payment.method = Objects.requireNonNull(method, "method is required");
        return payment;
    }

    public Order getOrder() { return order; }
    public String getProvider() { return provider; }
    public String getProviderOrderId() { return providerOrderId; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public BigDecimal getAmount() { return amount; }
    public String getMethod() { return method; }
    public String getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
}
