package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "delivery_partners", uniqueConstraints =
        @UniqueConstraint(name = "uk_delivery_partners_user", columnNames = "user_id"))
public class DeliveryPartner extends AuditableEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "vehicle_type", nullable = false, length = 32)
    private String vehicleType;

    @Column(name = "vehicle_number", nullable = false, length = 24)
    private String vehicleNumber;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_location_id")
    private Location currentLocation;

    @Column(name = "avg_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO.setScale(2);

    @Column(name = "completed_deliveries", nullable = false)
    private Integer completedDeliveries = 0;

    protected DeliveryPartner() { }

    public static DeliveryPartner create(User user, String vehicleType, String vehicleNumber) {
        DeliveryPartner partner = new DeliveryPartner();
        partner.user = Objects.requireNonNull(user, "user is required");
        partner.vehicleType = Objects.requireNonNull(vehicleType, "vehicleType is required");
        partner.vehicleNumber = Objects.requireNonNull(vehicleNumber, "vehicleNumber is required");
        return partner;
    }

    public User getUser() { return user; }
    public String getVehicleType() { return vehicleType; }
    public String getVehicleNumber() { return vehicleNumber; }
    public String getStatus() { return status; }
    public boolean isAvailable() { return available; }
    public Location getCurrentLocation() { return currentLocation; }
    public BigDecimal getAverageRating() { return averageRating; }
    public Integer getCompletedDeliveries() { return completedDeliveries; }
}
