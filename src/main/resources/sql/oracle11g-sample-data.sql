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
    1,
    'WAITING_EVENT_OPEN',
    100,
    600,
    'OPEN',
    'Y',
    SYSTIMESTAMP - INTERVAL '1' HOUR,
    SYSTIMESTAMP + INTERVAL '7' DAY,
    SYSTIMESTAMP,
    SYSTIMESTAMP
);

INSERT INTO product (product_id, category_id, product_name, price, stock_count, status, created_at, updated_at)
VALUES (1001, 10, 'HOSTING_BASIC', 9900, 10000, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO product (product_id, category_id, product_name, price, stock_count, status, created_at, updated_at)
VALUES (1002, 10, 'HOSTING_PRO', 19900, 5000, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO product (product_id, category_id, product_name, price, stock_count, status, created_at, updated_at)
VALUES (2001, 20, 'DOMAIN_STANDARD', 12000, 3000, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP);

INSERT INTO coupon_stock (
    coupon_id,
    coupon_name,
    total_count,
    remain_count,
    status,
    created_at,
    updated_at
) VALUES (
    1,
    'OPENING_COUPON',
    10000,
    10000,
    'ACTIVE',
    SYSTIMESTAMP,
    SYSTIMESTAMP
);

COMMIT;
