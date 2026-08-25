CREATE TABLE menu_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hotel_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    available TINYINT(1) NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    KEY idx_menu_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    hotel_id BIGINT NOT NULL,
    booking_id BIGINT NOT NULL,
    room_number VARCHAR(30) NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_food_orders_user (user_id),
    KEY idx_food_orders_hotel (hotel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE food_order_items (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    menu_item_id BIGINT NOT NULL,
    item_name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_food_order_items_order FOREIGN KEY (order_id) REFERENCES food_orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
