package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "road_edges", uniqueConstraints = @UniqueConstraint(
        name = "uk_road_edges_pair", columnNames = {"from_location_id", "to_location_id"}))
@Check(constraints = "distance_m > 0 and avg_speed_kmph > 0")
public class RoadEdge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_location_id", nullable = false)
    private Location fromLocation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_location_id", nullable = false)
    private Location toLocation;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceMeters;

    @Column(name = "avg_speed_kmph", nullable = false)
    private Integer averageSpeedKmph = 25;

    protected RoadEdge() { }

    public static RoadEdge create(Location fromLocation, Location toLocation, int distanceMeters) {
        RoadEdge edge = new RoadEdge();
        edge.fromLocation = Objects.requireNonNull(fromLocation, "fromLocation is required");
        edge.toLocation = Objects.requireNonNull(toLocation, "toLocation is required");
        edge.setDistanceMeters(distanceMeters);
        return edge;
    }

    public Long getId() { return id; }
    public Location getFromLocation() { return fromLocation; }
    public Location getToLocation() { return toLocation; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getAverageSpeedKmph() { return averageSpeedKmph; }

    public void setDistanceMeters(int distanceMeters) {
        if (distanceMeters <= 0) throw new IllegalArgumentException("distanceMeters must be positive");
        this.distanceMeters = distanceMeters;
    }

    public void setAverageSpeedKmph(int averageSpeedKmph) {
        if (averageSpeedKmph <= 0) throw new IllegalArgumentException("averageSpeedKmph must be positive");
        this.averageSpeedKmph = averageSpeedKmph;
    }
}
