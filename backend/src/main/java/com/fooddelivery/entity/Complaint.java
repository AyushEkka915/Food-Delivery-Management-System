package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;
import com.fooddelivery.entity.enums.ComplaintAgainstType;
import com.fooddelivery.entity.enums.ComplaintStatus;
import org.hibernate.annotations.Check;

@Entity
@Table(name = "complaints")
@Check(constraints = "against_type in ('RESTAURANT', 'PARTNER', 'PLATFORM') and status in ('OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED')")
public class Complaint extends AuditableEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "raised_by_user_id", nullable = false)
    private User raisedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "against_type", nullable = false, length = 16)
    private ComplaintAgainstType againstType;

    @Column(name = "subject", nullable = false, length = 150)
    private String subject;

    @Column(name = "description", nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ComplaintStatus status = ComplaintStatus.OPEN;

    @Column(name = "resolution_note", length = 2000)
    private String resolutionNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    protected Complaint() { }

    public static Complaint create(Order order, User raisedBy, ComplaintAgainstType againstType, String subject,
            String description) {
        Complaint complaint = new Complaint();
        complaint.order = Objects.requireNonNull(order, "order is required");
        complaint.raisedBy = Objects.requireNonNull(raisedBy, "raisedBy is required");
        complaint.againstType = Objects.requireNonNull(againstType, "againstType is required");
        complaint.subject = requireText(subject, "subject", 150);
        complaint.description = requireText(description, "description", 2000);
        return complaint;
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        if (value.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return value;
    }

    public Order getOrder() { return order; }
    public User getRaisedBy() { return raisedBy; }
    public ComplaintAgainstType getAgainstType() { return againstType; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public ComplaintStatus getStatus() { return status; }
    public String getResolutionNote() { return resolutionNote; }
    public User getResolvedBy() { return resolvedBy; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
