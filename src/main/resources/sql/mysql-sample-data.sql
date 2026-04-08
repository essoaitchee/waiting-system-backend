INSERT INTO queue_event (
    event_id,
    event_name,
    capacity_per_second,
    admission_window_seconds,
    event_status,
    active_yn,
    starts_at,
    ends_at,
    created_at,
    updated_at
) VALUES (
    1001,
    'WAITING_EVENT_OPEN',
    100,
    600,
    'OPEN',
    'Y',
    CURRENT_TIMESTAMP(6) - INTERVAL 1 HOUR,
    CURRENT_TIMESTAMP(6) + INTERVAL 7 DAY,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
)
ON DUPLICATE KEY UPDATE
    event_name = VALUES(event_name),
    capacity_per_second = VALUES(capacity_per_second),
    admission_window_seconds = VALUES(admission_window_seconds),
    event_status = VALUES(event_status),
    active_yn = VALUES(active_yn),
    starts_at = VALUES(starts_at),
    ends_at = VALUES(ends_at),
    updated_at = VALUES(updated_at);

INSERT INTO product (product_id, category_id, product_name, price, stock_count, status, created_at, updated_at)
VALUES (1001, 10, 'HOSTING_BASIC', 9900, 10000, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    product_name = VALUES(product_name),
    price = VALUES(price),
    stock_count = VALUES(stock_count),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO product (product_id, category_id, product_name, price, stock_count, status, created_at, updated_at)
VALUES (1002, 10, 'HOSTING_PRO', 19900, 5000, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    product_name = VALUES(product_name),
    price = VALUES(price),
    stock_count = VALUES(stock_count),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO product (product_id, category_id, product_name, price, stock_count, status, created_at, updated_at)
VALUES (2001, 20, 'DOMAIN_STANDARD', 12000, 3000, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    product_name = VALUES(product_name),
    price = VALUES(price),
    stock_count = VALUES(stock_count),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO coupon_stock (
    coupon_id,
    coupon_name,
    total_count,
    remain_count,
    status,
    created_at,
    updated_at
) VALUES
    (201, '웰컴쿠폰', 500, 500, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    (202, '주말특가쿠폰', 300, 300, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
ON DUPLICATE KEY UPDATE
    coupon_name = VALUES(coupon_name),
    total_count = VALUES(total_count),
    remain_count = VALUES(remain_count),
    status = VALUES(status),
    updated_at = VALUES(updated_at);
