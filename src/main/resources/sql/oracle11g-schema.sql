CREATE TABLE queue_event (
    event_id NUMBER(19) PRIMARY KEY,
    event_name VARCHAR2(100) NOT NULL,
    capacity_per_second NUMBER(10) DEFAULT 100 NOT NULL,
    admission_window_seconds NUMBER(10) DEFAULT 600 NOT NULL,
    event_status VARCHAR2(20) DEFAULT 'OPEN' NOT NULL,
    active_yn CHAR(1) DEFAULT 'Y' NOT NULL,
    starts_at TIMESTAMP NULL,
    ends_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT chk_queue_event_status CHECK (event_status IN ('OPEN', 'CLOSED')),
    CONSTRAINT chk_queue_event_active CHECK (active_yn IN ('Y', 'N'))
);

CREATE TABLE queue_entry (
    queue_entry_id NUMBER(19) PRIMARY KEY,
    event_id NUMBER(19) NOT NULL,
    user_id VARCHAR2(100) NOT NULL,
    queue_token VARCHAR2(64) NOT NULL,
    queue_sequence NUMBER(19) NOT NULL,
    status VARCHAR2(20) NOT NULL,
    entered_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    admission_token VARCHAR2(64) NULL,
    admitted_at TIMESTAMP NULL,
    admission_expires_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_queue_entry_event FOREIGN KEY (event_id) REFERENCES queue_event (event_id),
    CONSTRAINT uk_queue_entry_event_user UNIQUE (event_id, user_id),
    CONSTRAINT uk_queue_entry_queue_token UNIQUE (queue_token),
    CONSTRAINT uk_queue_entry_admission_token UNIQUE (admission_token),
    CONSTRAINT chk_queue_entry_status CHECK (status IN ('WAITING', 'ADMITTED', 'ENTERED', 'EXPIRED'))
);

CREATE TABLE product (
    product_id NUMBER(19) PRIMARY KEY,
    category_id NUMBER(19) NOT NULL,
    product_name VARCHAR2(200) NOT NULL,
    price NUMBER(10) NOT NULL,
    stock_count NUMBER(10) DEFAULT 0 NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT chk_product_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE coupon_stock (
    coupon_id NUMBER(19) PRIMARY KEY,
    coupon_name VARCHAR2(100) NOT NULL,
    total_count NUMBER(10) NOT NULL,
    remain_count NUMBER(10) NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT chk_coupon_stock_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_coupon_stock_count CHECK (total_count >= 0 AND remain_count >= 0 AND remain_count <= total_count)
);

CREATE TABLE coupon_issue (
    coupon_issue_id NUMBER(19) PRIMARY KEY,
    coupon_id NUMBER(19) NOT NULL,
    user_id VARCHAR2(100) NOT NULL,
    issued_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT fk_coupon_issue_stock FOREIGN KEY (coupon_id) REFERENCES coupon_stock (coupon_id),
    CONSTRAINT uk_coupon_issue_coupon_user UNIQUE (coupon_id, user_id)
);

CREATE TABLE demo_click_record (
    demo_click_record_id NUMBER(19) PRIMARY KEY,
    user_id VARCHAR2(100) NOT NULL,
    coupon_id NUMBER(19) NULL,
    coupon_name VARCHAR2(100) NULL,
    reaction_time_ms NUMBER(19) NOT NULL,
    clicked_at TIMESTAMP NOT NULL,
    round_started_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

CREATE SEQUENCE seq_queue_entry START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_coupon_issue START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE seq_demo_click_record START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE INDEX idx_queue_event_open ON queue_event (active_yn, event_status, starts_at, ends_at);
CREATE INDEX idx_queue_entry_waiting ON queue_entry (event_id, status, queue_sequence);
CREATE INDEX idx_product_list ON product (status, category_id, product_id);
CREATE INDEX idx_coupon_issue_lookup ON coupon_issue (coupon_id, user_id);
CREATE INDEX idx_coupon_issue_user_issued ON coupon_issue (user_id, issued_at);
CREATE UNIQUE INDEX uk_demo_click_record_user_round ON demo_click_record (user_id, round_started_at);
CREATE INDEX idx_demo_click_record_ranking ON demo_click_record (reaction_time_ms, clicked_at);
