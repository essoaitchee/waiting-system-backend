CREATE TABLE queue_event (
    event_id BIGINT PRIMARY KEY,
    event_name VARCHAR(100) NOT NULL,
    capacity_per_second INT NOT NULL DEFAULT 100,
    admission_window_seconds INT NOT NULL DEFAULT 600,
    event_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    active_yn CHAR(1) NOT NULL DEFAULT 'Y',
    starts_at DATETIME(6) NULL,
    ends_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_queue_event_status CHECK (event_status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_queue_event_active CHECK (active_yn IN ('Y', 'N'))
) ENGINE=InnoDB;

CREATE TABLE queue_entry (
    queue_entry_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    queue_token VARCHAR(64) NOT NULL,
    queue_sequence BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    entered_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    admission_token VARCHAR(64) NULL,
    admitted_at DATETIME(6) NULL,
    admission_expires_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_queue_entry_event FOREIGN KEY (event_id) REFERENCES queue_event (event_id),
    CONSTRAINT uk_queue_entry_event_user UNIQUE (event_id, user_id),
    CONSTRAINT uk_queue_entry_queue_token UNIQUE (queue_token),
    CONSTRAINT uk_queue_entry_admission_token UNIQUE (admission_token),
    CONSTRAINT chk_queue_entry_status CHECK (status IN ('WAITING', 'ADMITTED', 'ENTERED', 'EXPIRED'))
) ENGINE=InnoDB;

CREATE TABLE product (
    product_id BIGINT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    price INT NOT NULL,
    stock_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
) ENGINE=InnoDB;

CREATE TABLE coupon_stock (
    coupon_id BIGINT PRIMARY KEY,
    coupon_name VARCHAR(100) NOT NULL,
    total_count INT NOT NULL,
    remain_count INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_coupon_stock_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_coupon_stock_count CHECK (total_count >= 0 AND remain_count >= 0 AND remain_count <= total_count)
) ENGINE=InnoDB;

CREATE TABLE coupon_issue (
    coupon_issue_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id VARCHAR(100) NOT NULL,
    issued_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_coupon_issue_stock FOREIGN KEY (coupon_id) REFERENCES coupon_stock (coupon_id),
    CONSTRAINT uk_coupon_issue_coupon_user UNIQUE (coupon_id, user_id)
) ENGINE=InnoDB;

CREATE TABLE demo_click_record (
    demo_click_record_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(100) NOT NULL,
    coupon_id BIGINT NULL,
    coupon_name VARCHAR(100) NULL,
    reaction_time_ms BIGINT NOT NULL,
    clicked_at DATETIME(6) NOT NULL,
    round_started_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB;

CREATE INDEX idx_queue_event_open ON queue_event (active_yn, event_status, starts_at, ends_at);
CREATE INDEX idx_queue_entry_waiting ON queue_entry (event_id, status, queue_sequence);
CREATE INDEX idx_product_list ON product (status, category_id, product_id);
CREATE INDEX idx_coupon_issue_lookup ON coupon_issue (coupon_id, user_id);
CREATE INDEX idx_coupon_issue_user_issued ON coupon_issue (user_id, issued_at);
CREATE UNIQUE INDEX uk_demo_click_record_user_round ON demo_click_record (user_id, round_started_at);
CREATE INDEX idx_demo_click_record_ranking ON demo_click_record (reaction_time_ms, clicked_at);
