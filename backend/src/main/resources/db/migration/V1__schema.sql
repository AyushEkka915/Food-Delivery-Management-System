-- ============================================================================
-- Smart Food Delivery Management System
-- V1__schema.sql  (Flyway migration)
--
-- Hand-written DDL. This file is the database component of the project:
-- 22 tables, primary/foreign keys, unique constraints, check constraints and
-- indexes. Hibernate is configured with ddl-auto=validate, so the entities
-- must match this schema exactly.
--
-- Conventions:
--   * BIGINT surrogate primary keys.
--   * DECIMAL(10,2) for money; never DOUBLE/FLOAT.
--   * DATETIME(6) UTC for timestamps.
--   * created_at / updated_at on every mutable table.
--   * snake_case columns; utf8mb4 / utf8mb4_0900_ai_ci.
--   * ON DELETE RESTRICT unless the child is a pure snapshot/optional ref.
-- ============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------------------------------------------------------
-- 1. roles
-- ----------------------------------------------------------------------------
CREATE TABLE roles (
    id   BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 2. users
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(190) NOT NULL,
    phone         VARCHAR(20)  NOT NULL,
    password_hash VARCHAR(80)  NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    created_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    UNIQUE KEY uk_users_phone (phone),
    KEY idx_users_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 3. user_roles (M:N)
-- ----------------------------------------------------------------------------
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 4. refresh_tokens (rotating, hashed, revocable)
-- ----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked    TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 5. locations (graph vertices)
-- ----------------------------------------------------------------------------
CREATE TABLE locations (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    name      VARCHAR(120) NOT NULL,
    latitude  DECIMAL(9,6) NULL,
    longitude DECIMAL(9,6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_locations_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 6. road_edges (graph edges, undirected -> both directions stored)
-- ----------------------------------------------------------------------------
CREATE TABLE road_edges (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    from_location_id BIGINT NOT NULL,
    to_location_id   BIGINT NOT NULL,
    distance_m       INT    NOT NULL,
    avg_speed_kmph   INT    NOT NULL DEFAULT 25,
    PRIMARY KEY (id),
    UNIQUE KEY uk_road_edges_pair (from_location_id, to_location_id),
    KEY idx_road_edges_from (from_location_id),
    KEY idx_road_edges_to   (to_location_id),
    CONSTRAINT fk_road_edges_from FOREIGN KEY (from_location_id) REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT fk_road_edges_to   FOREIGN KEY (to_location_id)   REFERENCES locations (id) ON DELETE RESTRICT,
    CONSTRAINT chk_road_edges_distance CHECK (distance_m > 0),
    CONSTRAINT chk_road_edges_speed    CHECK (avg_speed_kmph > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 7. addresses (customer addresses; snapshotted into orders)
-- ----------------------------------------------------------------------------
CREATE TABLE addresses (
    id          BIGINT NOT NULL AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    location_id BIGINT NULL,
    label       VARCHAR(32)  NOT NULL,
    line1       VARCHAR(190) NOT NULL,
    line2       VARCHAR(190) NULL,
    city        VARCHAR(80)  NOT NULL,
    pincode     VARCHAR(12)  NOT NULL,
    latitude    DECIMAL(9,6) NULL,
    longitude   DECIMAL(9,6) NULL,
    is_default  TINYINT(1)   NOT NULL DEFAULT 0,
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_addresses_user (user_id),
    CONSTRAINT fk_addresses_user     FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_addresses_location FOREIGN KEY (location_id) REFERENCES locations (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 8. restaurants
-- ----------------------------------------------------------------------------
CREATE TABLE restaurants (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    owner_user_id    BIGINT NOT NULL,
    name             VARCHAR(150) NOT NULL,
    description      VARCHAR(500) NULL,
    phone            VARCHAR(20)  NOT NULL,
    address_line     VARCHAR(190) NOT NULL,
    city             VARCHAR(80)  NOT NULL,
    location_id      BIGINT NULL,
    cuisine          VARCHAR(80)  NOT NULL,
    status           VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    opening_time     TIME NULL,
    closing_time     TIME NULL,
    commission_rate  DECIMAL(5,2) NOT NULL DEFAULT 12.00,
    rating_avg       DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count     INT          NOT NULL DEFAULT 0,
    created_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_restaurants_owner (owner_user_id),
    KEY idx_restaurants_status (status),
    KEY idx_restaurants_location (location_id),
    KEY idx_restaurants_cuisine (cuisine),
    CONSTRAINT fk_restaurants_owner    FOREIGN KEY (owner_user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_restaurants_location FOREIGN KEY (location_id)  REFERENCES locations (id) ON DELETE SET NULL,
    CONSTRAINT chk_restaurants_commission CHECK (commission_rate >= 0 AND commission_rate <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 9. food_categories (global taxonomy)
-- ----------------------------------------------------------------------------
CREATE TABLE food_categories (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    name          VARCHAR(60) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 10. food_items
-- ----------------------------------------------------------------------------
CREATE TABLE food_items (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    restaurant_id BIGINT NOT NULL,
    category_id   BIGINT NOT NULL,
    name          VARCHAR(150) NOT NULL,
    description   VARCHAR(500) NULL,
    price         DECIMAL(10,2) NOT NULL,
    is_veg        TINYINT(1) NOT NULL DEFAULT 0,
    is_available  TINYINT(1) NOT NULL DEFAULT 1,
    stock_qty     INT NOT NULL DEFAULT 0,
    prep_time_min INT NOT NULL DEFAULT 15,
    image_url     VARCHAR(500) NULL,
    rating_avg    DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    rating_count  INT NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_food_items_restaurant_name (restaurant_id, name),
    KEY idx_food_items_restaurant (restaurant_id),
    KEY idx_food_items_restaurant_available (restaurant_id, is_available),
    KEY idx_food_items_category (category_id),
    CONSTRAINT fk_food_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE CASCADE,
    CONSTRAINT fk_food_items_category   FOREIGN KEY (category_id)   REFERENCES food_categories (id) ON DELETE RESTRICT,
    CONSTRAINT chk_food_items_price     CHECK (price >= 0),
    CONSTRAINT chk_food_items_stock     CHECK (stock_qty >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 11. cart_items (server-side cart, single restaurant enforced in service)
-- ----------------------------------------------------------------------------
CREATE TABLE cart_items (
    id           BIGINT NOT NULL AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    food_item_id BIGINT NOT NULL,
    quantity     INT NOT NULL,
    added_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_cart_items_user_item (user_id, food_item_id),
    KEY idx_cart_items_user (user_id),
    CONSTRAINT fk_cart_items_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_cart_items_item FOREIGN KEY (food_item_id) REFERENCES food_items (id) ON DELETE CASCADE,
    CONSTRAINT chk_cart_items_qty CHECK (quantity > 0 AND quantity <= 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 12. orders
-- ----------------------------------------------------------------------------
CREATE TABLE orders (
    id               BIGINT NOT NULL AUTO_INCREMENT,
    order_number     VARCHAR(24) NOT NULL,
    customer_id      BIGINT NOT NULL,
    restaurant_id    BIGINT NOT NULL,
    address_id       BIGINT NULL,
    address_snapshot JSON NOT NULL,
    delivery_location_id BIGINT NULL,
    status           VARCHAR(24) NOT NULL DEFAULT 'PLACED',
    subtotal         DECIMAL(10,2) NOT NULL,
    discount         DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    tax              DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    delivery_fee     DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    total            DECIMAL(10,2) NOT NULL,
    coupon_id        BIGINT NULL,
    payment_method   VARCHAR(16) NOT NULL,
    placed_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confirmed_at     DATETIME(6) NULL,
    prepared_at      DATETIME(6) NULL,
    ready_at         DATETIME(6) NULL,
    delivered_at     DATETIME(6) NULL,
    cancelled_at     DATETIME(6) NULL,
    cancel_reason    VARCHAR(255) NULL,
    version          BIGINT NOT NULL DEFAULT 0,
    created_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_orders_number (order_number),
    KEY idx_orders_customer_placed (customer_id, placed_at),
    KEY idx_orders_restaurant_status (restaurant_id, status),
    KEY idx_orders_status (status),
    KEY idx_orders_placed_at (placed_at),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE RESTRICT,
    CONSTRAINT fk_orders_address FOREIGN KEY (address_id) REFERENCES addresses (id) ON DELETE SET NULL,
    CONSTRAINT fk_orders_location FOREIGN KEY (delivery_location_id) REFERENCES locations (id) ON DELETE SET NULL,
    CONSTRAINT chk_orders_money CHECK (subtotal >= 0 AND discount >= 0 AND tax >= 0 AND delivery_fee >= 0 AND total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 13. order_items (with name + price snapshot)
-- ----------------------------------------------------------------------------
CREATE TABLE order_items (
    id                 BIGINT NOT NULL AUTO_INCREMENT,
    order_id           BIGINT NOT NULL,
    food_item_id       BIGINT NULL,
    item_name_snapshot  VARCHAR(150) NOT NULL,
    unit_price_snapshot DECIMAL(10,2) NOT NULL,
    quantity           INT NOT NULL,
    line_total         DECIMAL(10,2) NOT NULL,
    notes              VARCHAR(255) NULL,
    PRIMARY KEY (id),
    KEY idx_order_items_order (order_id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_items_item  FOREIGN KEY (food_item_id) REFERENCES food_items (id) ON DELETE SET NULL,
    CONSTRAINT chk_order_items_qty  CHECK (quantity > 0),
    CONSTRAINT chk_order_items_price CHECK (unit_price_snapshot >= 0 AND line_total >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 14. payments
-- ----------------------------------------------------------------------------
CREATE TABLE payments (
    id                  BIGINT NOT NULL AUTO_INCREMENT,
    order_id            BIGINT NOT NULL,
    provider            VARCHAR(32) NOT NULL,
    provider_order_id   VARCHAR(128) NULL,
    provider_payment_id VARCHAR(128) NULL,
    idempotency_key     VARCHAR(64) NOT NULL,
    amount              DECIMAL(10,2) NOT NULL,
    method              VARCHAR(16) NOT NULL,
    status              VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    failure_reason      VARCHAR(255) NULL,
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_payments_idempotency (idempotency_key),
    UNIQUE KEY uk_payments_provider_payment (provider_payment_id),
    KEY idx_payments_order (order_id),
    KEY idx_payments_status (status),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT chk_payments_amount CHECK (amount >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 15. delivery_partners
-- ----------------------------------------------------------------------------
CREATE TABLE delivery_partners (
    id                   BIGINT NOT NULL AUTO_INCREMENT,
    user_id              BIGINT NOT NULL,
    vehicle_type         VARCHAR(32) NOT NULL,
    vehicle_number       VARCHAR(24) NOT NULL,
    status               VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    is_available         TINYINT(1) NOT NULL DEFAULT 0,
    current_location_id  BIGINT NULL,
    avg_rating           DECIMAL(3,2) NOT NULL DEFAULT 0.00,
    completed_deliveries  INT NOT NULL DEFAULT 0,
    created_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at           DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_partners_user (user_id),
    KEY idx_delivery_partners_available (is_available, status),
    CONSTRAINT fk_delivery_partners_user     FOREIGN KEY (user_id)             REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_delivery_partners_location FOREIGN KEY (current_location_id) REFERENCES locations (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 16. delivery_assignments (one per order, route frozen at assignment time)
-- ----------------------------------------------------------------------------
CREATE TABLE delivery_assignments (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    order_id       BIGINT NOT NULL,
    partner_id     BIGINT NOT NULL,
    status         VARCHAR(24) NOT NULL DEFAULT 'ASSIGNED',
    assigned_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    accepted_at    DATETIME(6) NULL,
    picked_up_at   DATETIME(6) NULL,
    delivered_at   DATETIME(6) NULL,
    declined_at    DATETIME(6) NULL,
    route_distance_m INT NULL,
    route_eta_min    INT NULL,
    route_path       JSON NULL,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_delivery_assignments_order (order_id),
    KEY idx_delivery_assignments_partner (partner_id, status),
    CONSTRAINT fk_delivery_assignments_order   FOREIGN KEY (order_id)   REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_assignments_partner FOREIGN KEY (partner_id) REFERENCES delivery_partners (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 17. order_status_history (append-only)
-- ----------------------------------------------------------------------------
CREATE TABLE order_status_history (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    from_status     VARCHAR(24) NULL,
    to_status       VARCHAR(24) NOT NULL,
    changed_by_user BIGINT NULL,
    note            VARCHAR(255) NULL,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_order_status_history_order (order_id, created_at),
    CONSTRAINT fk_order_status_history_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_status_history_user  FOREIGN KEY (changed_by_user) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 18. reviews (one per delivered order)
-- ----------------------------------------------------------------------------
CREATE TABLE reviews (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    order_id      BIGINT NOT NULL,
    customer_id   BIGINT NOT NULL,
    restaurant_id BIGINT NOT NULL,
    rating        TINYINT NOT NULL,
    comment       VARCHAR(1000) NULL,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_reviews_order (order_id),
    KEY idx_reviews_restaurant (restaurant_id, created_at),
    CONSTRAINT fk_reviews_order      FOREIGN KEY (order_id)      REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_reviews_customer   FOREIGN KEY (customer_id)   REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_reviews_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id) ON DELETE RESTRICT,
    CONSTRAINT chk_reviews_rating    CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 19. coupons
-- ----------------------------------------------------------------------------
CREATE TABLE coupons (
    id              BIGINT NOT NULL AUTO_INCREMENT,
    code            VARCHAR(32) NOT NULL,
    type            VARCHAR(16) NOT NULL,
    value           DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    max_discount    DECIMAL(10,2) NULL,
    valid_from      DATETIME(6) NOT NULL,
    valid_to        DATETIME(6) NOT NULL,
    usage_limit     INT NULL,
    used_count      INT NOT NULL DEFAULT 0,
    per_user_limit  INT NULL,
    first_order_only TINYINT(1) NOT NULL DEFAULT 0,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupons_code (code),
    KEY idx_coupons_active (is_active, valid_to),
    CONSTRAINT chk_coupons_type   CHECK (type IN ('PERCENT', 'FLAT')),
    CONSTRAINT chk_coupons_value  CHECK (value >= 0),
    CONSTRAINT chk_coupons_window CHECK (valid_to > valid_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 20. coupon_redemptions (enforces per-user limits)
-- ----------------------------------------------------------------------------
CREATE TABLE coupon_redemptions (
    id        BIGINT NOT NULL AUTO_INCREMENT,
    coupon_id BIGINT NOT NULL,
    user_id   BIGINT NOT NULL,
    order_id  BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_coupon_redemptions_coupon_order (coupon_id, order_id),
    KEY idx_coupon_redemptions_coupon_user (coupon_id, user_id),
    CONSTRAINT fk_coupon_redemptions_coupon FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE RESTRICT,
    CONSTRAINT fk_coupon_redemptions_user   FOREIGN KEY (user_id)   REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_coupon_redemptions_order  FOREIGN KEY (order_id)  REFERENCES orders (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 21. complaints
-- ----------------------------------------------------------------------------
CREATE TABLE complaints (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    order_id          BIGINT NOT NULL,
    raised_by_user_id BIGINT NOT NULL,
    against_type      VARCHAR(16) NOT NULL,
    subject           VARCHAR(150) NOT NULL,
    description       VARCHAR(2000) NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    resolution_note   VARCHAR(2000) NULL,
    resolved_by       BIGINT NULL,
    resolved_at       DATETIME(6) NULL,
    created_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_complaints_status (status),
    KEY idx_complaints_raised_by (raised_by_user_id),
    KEY idx_complaints_order (order_id),
    CONSTRAINT fk_complaints_order      FOREIGN KEY (order_id)          REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_complaints_raised_by  FOREIGN KEY (raised_by_user_id)  REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT fk_complaints_resolved_by FOREIGN KEY (resolved_by)      REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT chk_complaints_against   CHECK (against_type IN ('RESTAURANT', 'PARTNER', 'PLATFORM')),
    CONSTRAINT chk_complaints_status   CHECK (status IN ('OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ----------------------------------------------------------------------------
-- 22. audit_logs (durable record of admin / system actions)
-- ----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id            BIGINT NOT NULL AUTO_INCREMENT,
    actor_user_id BIGINT NULL,
    action        VARCHAR(64) NOT NULL,
    entity_type   VARCHAR(32) NOT NULL,
    entity_id     VARCHAR(64) NOT NULL,
    payload_json  JSON NULL,
    reversible    TINYINT(1) NOT NULL DEFAULT 0,
    created_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_audit_logs_created (created_at),
    KEY idx_audit_logs_entity (entity_type, entity_id),
    KEY idx_audit_logs_actor (actor_user_id),
    CONSTRAINT fk_audit_logs_actor FOREIGN KEY (actor_user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

SET FOREIGN_KEY_CHECKS = 1;

-- Deferred foreign keys (referenced table is created later in this file) ------
ALTER TABLE orders
    ADD CONSTRAINT fk_orders_coupon
    FOREIGN KEY (coupon_id) REFERENCES coupons (id) ON DELETE SET NULL;
