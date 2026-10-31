package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "delivery_assignments", uniqueConstraints =
        @UniqueConstraint(name = "uk_delivery_assignments_order", columnNames = "order_id"))
public class DeliveryAssignment extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "partner_id", nullable = false)
    private DeliveryPartner partner;

    @Column(name = "status", nullable = false, length = 24)
    private String status = "ASSIGNED";

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

    @Column(name = "picked_up_at")
    private LocalDateTime pickedUpAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "declined_at")
    private LocalDateTime declinedAt;

    @Column(name = "route_distance_m")
    private Integer routeDistanceMeters;

    @Column(name = "route_eta_min")
    private Integer routeEtaMinutes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "route_path", columnDefinition = "JSON")
    private Map<String, Object> routePath;

    protected DeliveryAssignment() { }

    public static DeliveryAssignment create(Order order, DeliveryPartner partner) {
        DeliveryAssignment assignment = new DeliveryAssignment();
        assignment.order = Objects.requireNonNull(order, "order is required");
        assignment.partner = Objects.requireNonNull(partner, "partner is required");
        assignment.assignedAt = LocalDateTime.now();
        return assignment;
    }

    public Order getOrder() { return order; }
    public DeliveryPartner getPartner() { return partner; }
    public String getStatus() { return status; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public LocalDateTime getDeclinedAt() { return declinedAt; }
    public Integer getRouteDistanceMeters() { return routeDistanceMeters; }
    public Integer getRouteEtaMinutes() { return routeEtaMinutes; }
    public Map<String, Object> getRoutePath() { return routePath; }
}
