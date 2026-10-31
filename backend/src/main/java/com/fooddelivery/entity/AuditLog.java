package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLog extends CreatedOnlyEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 32)
    private String entityType;

    @Column(name = "entity_id", nullable = false, length = 64)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload_json", columnDefinition = "JSON")
    private Map<String, Object> payloadJson;

    @Column(name = "reversible", nullable = false)
    private boolean reversible;

    protected AuditLog() { }

    public static AuditLog create(User actor, String action, String entityType, String entityId,
            Map<String, Object> payloadJson, boolean reversible) {
        AuditLog log = new AuditLog();
        log.actor = actor;
        log.action = requireText(action, "action", 64);
        log.entityType = requireText(entityType, "entityType", 32);
        log.entityId = requireText(entityId, "entityId", 64);
        log.payloadJson = payloadJson;
        log.reversible = reversible;
        return log;
    }

    private static String requireText(String value, String field, int maxLength) {
        Objects.requireNonNull(value, field + " is required");
        if (value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
        if (value.length() > maxLength) throw new IllegalArgumentException(field + " exceeds " + maxLength + " characters");
        return value;
    }

    public User getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Map<String, Object> getPayloadJson() { return payloadJson; }
    public boolean isReversible() { return reversible; }
}
