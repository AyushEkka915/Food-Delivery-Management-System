package com.fooddelivery.entity;

import com.fooddelivery.entity.enums.RestaurantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "restaurants")
public class Restaurant extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_user_id", nullable = false, unique = true) private User owner;
    @Column(nullable = false, length = 150) private String name;
    @Column(length = 500) private String description;
    @Column(nullable = false, length = 20) private String phone;
    @Column(name = "address_line", nullable = false, length = 190) private String addressLine;
    @Column(nullable = false, length = 80) private String city;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "location_id") private Location location;
    @Column(nullable = false, length = 80) private String cuisine;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private RestaurantStatus status = RestaurantStatus.PENDING;
    @Column(name = "opening_time") private LocalTime openingTime;
    @Column(name = "closing_time") private LocalTime closingTime;
    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 2) private BigDecimal commissionRate;
    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2) private BigDecimal ratingAverage;
    @Column(name = "rating_count", nullable = false) private Integer ratingCount;
    @OneToMany(mappedBy = "restaurant", fetch = FetchType.LAZY) private List<FoodItem> foodItems = new ArrayList<>();
    protected Restaurant() { }

    public static Restaurant createProfile(User owner, Location location, String name, String description, String phone,
            String addressLine, String city, String cuisine, LocalTime openingTime, LocalTime closingTime) {
        Restaurant restaurant = new Restaurant();
        restaurant.owner = Objects.requireNonNull(owner, "owner is required");
        restaurant.location = location;
        restaurant.name = name;
        restaurant.description = description;
        restaurant.phone = phone;
        restaurant.addressLine = addressLine;
        restaurant.city = city;
        restaurant.cuisine = cuisine;
        restaurant.openingTime = openingTime;
        restaurant.closingTime = closingTime;
        restaurant.commissionRate = new BigDecimal("12.00");
        restaurant.ratingAverage = BigDecimal.ZERO.setScale(2);
        restaurant.ratingCount = 0;
        return restaurant;
    }

    public String getDescription() { return description; }
    public String getPhone() { return phone; }
    public String getAddressLine() { return addressLine; }
    public String getCity() { return city; }
    public Location getLocation() { return location; }
    public User getOwner() { return owner; }
    public String getName() { return name; }
    public String getCuisine() { return cuisine; }
    public RestaurantStatus getStatus() { return status; }
    public boolean canTransitionStatusTo(RestaurantStatus nextStatus) {
        return (status == RestaurantStatus.PENDING
                    && (nextStatus == RestaurantStatus.APPROVED || nextStatus == RestaurantStatus.SUSPENDED))
                || (status == RestaurantStatus.APPROVED && nextStatus == RestaurantStatus.SUSPENDED);
    }

    public void transitionStatusTo(RestaurantStatus nextStatus) {
        if (!canTransitionStatusTo(nextStatus)) {
            throw new IllegalStateException("Restaurant status transition is not allowed");
        }
        status = nextStatus;
    }

    public LocalTime getOpeningTime() { return openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public BigDecimal getRatingAverage() { return ratingAverage; }
    public Integer getRatingCount() { return ratingCount; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public void setCity(String city) { this.city = city; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
}
