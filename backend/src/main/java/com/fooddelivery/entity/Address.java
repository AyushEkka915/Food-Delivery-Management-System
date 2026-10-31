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
@Table(name = "addresses")
public class Address extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(nullable = false, length = 32)
    private String label;

    @Column(name = "line1", nullable = false, length = 190)
    private String line1;

    @Column(name = "line2", length = 190)
    private String line2;

    @Column(nullable = false, length = 80)
    private String city;

    @Column(nullable = false, length = 12)
    private String pincode;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    protected Address() { }

    public static Address create(User user, Location location, String label, String line1, String line2, String city,
            String pincode, BigDecimal latitude, BigDecimal longitude, boolean defaultAddress) {
        Address address = new Address();
        address.user = Objects.requireNonNull(user, "user is required");
        address.location = location;
        address.label = requireText(label, "label", 32);
        address.line1 = requireText(line1, "line1", 190);
        address.line2 = validateOptionalText(line2, "line2", 190);
        address.city = requireText(city, "city", 80);
        address.pincode = requireText(pincode, "pincode", 12);
        address.latitude = validateCoordinate(latitude, "latitude", new BigDecimal("-90"), new BigDecimal("90"));
        address.longitude = validateCoordinate(longitude, "longitude", new BigDecimal("-180"), new BigDecimal("180"));
        address.defaultAddress = defaultAddress;
        return address;
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

    private static BigDecimal validateCoordinate(BigDecimal coordinate, String field, BigDecimal minimum,
            BigDecimal maximum) {
        if (coordinate == null) return null;
        if (coordinate.compareTo(minimum) < 0 || coordinate.compareTo(maximum) > 0) {
            throw new IllegalArgumentException(field + " is outside the allowed range");
        }
        BigDecimal normalized = coordinate.stripTrailingZeros();
        if (normalized.scale() > 6 || normalized.precision() - normalized.scale() > 3) {
            throw new IllegalArgumentException(field + " must fit DECIMAL(9,6)");
        }
        return coordinate;
    }

    public User getUser() { return user; }
    public Location getLocation() { return location; }
    public String getLabel() { return label; }
    public String getLine1() { return line1; }
    public String getLine2() { return line2; }
    public String getCity() { return city; }
    public String getPincode() { return pincode; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public boolean isDefaultAddress() { return defaultAddress; }
}
