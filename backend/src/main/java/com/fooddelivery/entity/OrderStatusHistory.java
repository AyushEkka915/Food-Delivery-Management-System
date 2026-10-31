package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "order_status_history")
public class OrderStatusHistory extends CreatedOnlyEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "from_status", length = 24)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 24)
    private String toStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user")
    private User changedBy;

    @Column(name = "note", length = 255)
    private String note;

    protected OrderStatusHistory() { }

    public static OrderStatusHistory create(Order order, String fromStatus, String toStatus, User changedBy,
            String note) {
        OrderStatusHistory history = new OrderStatusHistory();
        history.order = Objects.requireNonNull(order, "order is required");
        history.fromStatus = fromStatus;
        history.toStatus = Objects.requireNonNull(toStatus, "toStatus is required");
        history.changedBy = changedBy;
        history.note = note;
        return history;
    }

    public Long getId() { return super.getId(); }
    public Order getOrder() { return order; }
    public String getFromStatus() { return fromStatus; }
    public String getToStatus() { return toStatus; }
    public User getChangedBy() { return changedBy; }
    public String getNote() { return note; }
}
