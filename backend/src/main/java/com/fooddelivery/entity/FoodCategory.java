package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "food_categories")
public class FoodCategory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, unique = true, length = 60) private String name;
    @Column(name = "display_order", nullable = false) private Integer displayOrder;
    protected FoodCategory() { }
    public Long getId() { return id; }
    public String getName() { return name; }
    public Integer getDisplayOrder() { return displayOrder; }
}
