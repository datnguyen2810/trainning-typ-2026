-- =============================================================
-- TICKET BOOKING - DATABASE INITIALIZATION
-- MySQL 8.0+
-- =============================================================

CREATE DATABASE IF NOT EXISTS ticket_booking
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE ticket_booking;

-- =============================================================
-- 1. TABLES
-- =============================================================

CREATE TABLE users (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    status          ENUM('ACTIVE', 'INACTIVE', 'BLOCKED') NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_phone UNIQUE (phone)
) ENGINE = InnoDB;

CREATE TABLE venues (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(150) NOT NULL,
    address         VARCHAR(255) NOT NULL,
    city            VARCHAR(100) NOT NULL,
    capacity        INT UNSIGNED NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_venues_capacity CHECK (capacity > 0)
) ENGINE = InnoDB;

CREATE TABLE events (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    venue_id        BIGINT UNSIGNED NOT NULL,
    name            VARCHAR(200) NOT NULL,
    description     TEXT,
    start_at        DATETIME NOT NULL,
    end_at          DATETIME NOT NULL,
    status          ENUM('DRAFT', 'PUBLISHED', 'CANCELLED', 'COMPLETED')
                    NOT NULL DEFAULT 'DRAFT',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_events_venue
        FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT chk_events_time CHECK (end_at > start_at)
) ENGINE = InnoDB;

CREATE TABLE seats (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    venue_id        BIGINT UNSIGNED NOT NULL,
    section_name    VARCHAR(50) NOT NULL,
    row_label       VARCHAR(10) NOT NULL,
    seat_number     INT UNSIGNED NOT NULL,
    seat_type       ENUM('STANDARD', 'VIP') NOT NULL DEFAULT 'STANDARD',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_seats_venue
        FOREIGN KEY (venue_id) REFERENCES venues (id),
    CONSTRAINT uq_seat_position
        UNIQUE (venue_id, section_name, row_label, seat_number),
    CONSTRAINT chk_seats_number CHECK (seat_number > 0)
) ENGINE = InnoDB;

-- Một ghế vật lý có thể được bán ở nhiều sự kiện khác nhau.
-- Trạng thái được đặt tại event_seats thay vì seats vì trạng thái phụ thuộc sự kiện.
CREATE TABLE event_seats (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    event_id        BIGINT UNSIGNED NOT NULL,
    seat_id         BIGINT UNSIGNED NOT NULL,
    price           DECIMAL(12, 2) NOT NULL,
    status          ENUM('AVAILABLE', 'HELD', 'BOOKED')
                    NOT NULL DEFAULT 'AVAILABLE',
    version         INT UNSIGNED NOT NULL DEFAULT 0,
    held_until      DATETIME NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_event_seats_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_seats_seat
        FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT uq_event_seat UNIQUE (event_id, seat_id),
    CONSTRAINT chk_event_seats_price CHECK (price >= 0)
) ENGINE = InnoDB;

CREATE TABLE bookings (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    booking_code    VARCHAR(30) NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    event_id        BIGINT UNSIGNED NOT NULL,
    status          ENUM('PENDING', 'CONFIRMED', 'CANCELLED', 'EXPIRED')
                    NOT NULL DEFAULT 'PENDING',
    total_amount    DECIMAL(12, 2) NOT NULL DEFAULT 0,
    expires_at      DATETIME NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                    ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_bookings_code UNIQUE (booking_code),
    CONSTRAINT fk_bookings_user
        FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_bookings_event
        FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT chk_bookings_total CHECK (total_amount >= 0)
) ENGINE = InnoDB;

CREATE TABLE booking_items (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    booking_id      BIGINT UNSIGNED NOT NULL,
    event_seat_id   BIGINT UNSIGNED NOT NULL,
    unit_price      DECIMAL(12, 2) NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_booking_items_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_booking_items_event_seat
        FOREIGN KEY (event_seat_id) REFERENCES event_seats (id),
    CONSTRAINT uq_booking_event_seat UNIQUE (booking_id, event_seat_id),
    CONSTRAINT chk_booking_items_price CHECK (unit_price >= 0)
) ENGINE = InnoDB;

CREATE TABLE payments (
    id                  BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    booking_id          BIGINT UNSIGNED NOT NULL,
    transaction_code    VARCHAR(100),
    payment_method      ENUM('CASH', 'BANK_TRANSFER', 'CARD', 'E_WALLET') NOT NULL,
    amount              DECIMAL(12, 2) NOT NULL,
    status              ENUM('PENDING', 'SUCCESS', 'FAILED', 'REFUNDED')
                        NOT NULL DEFAULT 'PENDING',
    paid_at             DATETIME NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_payments_transaction UNIQUE (transaction_code),
    CONSTRAINT fk_payments_booking
        FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_payments_amount CHECK (amount > 0)
) ENGINE = InnoDB;

-- =============================================================
-- 2. SAMPLE DATA
-- =============================================================

INSERT INTO users (id, email, password_hash, full_name, phone, role, status) VALUES
    (1, 'admin@ticket.local', 'DEMO_HASH_NOT_FOR_LOGIN', 'Quản trị viên', '0900000001', 'ADMIN', 'ACTIVE'),
    (2, 'an.nguyen@example.com', 'DEMO_HASH_NOT_FOR_LOGIN', 'Nguyễn Hoàng An', '0900000002', 'USER', 'ACTIVE'),
    (3, 'binh.tran@example.com', 'DEMO_HASH_NOT_FOR_LOGIN', 'Trần Gia Bình', '0900000003', 'USER', 'ACTIVE'),
    (4, 'chi.le@example.com', 'DEMO_HASH_NOT_FOR_LOGIN', 'Lê Minh Chi', '0900000004', 'USER', 'ACTIVE'),
    (5, 'dung.pham@example.com', 'DEMO_HASH_NOT_FOR_LOGIN', 'Phạm Anh Dũng', '0900000005', 'USER', 'ACTIVE'),
    (6, 'ha.vo@example.com', 'DEMO_HASH_NOT_FOR_LOGIN', 'Võ Thanh Hà', '0900000006', 'USER', 'ACTIVE');

INSERT INTO venues (id, name, address, city, capacity) VALUES
    (1, 'Nhà hát Thành phố', '07 Công Trường Lam Sơn, Quận 1', 'Hồ Chí Minh', 500),
    (2, 'Trung tâm Hội nghị Riverside', '360D Bến Vân Đồn, Quận 4', 'Hồ Chí Minh', 1000),
    (3, 'Cung Thể thao Quần Ngựa', '30 Văn Cao, Ba Đình', 'Hà Nội', 3500);

INSERT INTO events (id, venue_id, name, description, start_at, end_at, status) VALUES
    (1, 1, 'Đêm nhạc Mùa Thu', 'Đêm nhạc acoustic dành cho người yêu nhạc Việt.',
     '2026-10-18 19:30:00', '2026-10-18 22:00:00', 'PUBLISHED'),
    (2, 2, 'Vietnam Backend Conference 2026', 'Hội nghị dành cho lập trình viên backend.',
     '2026-11-07 08:00:00', '2026-11-07 17:30:00', 'PUBLISHED'),
    (3, 3, 'Giải bóng rổ All Stars', 'Trận đấu giao hữu giữa các ngôi sao.',
     '2026-11-21 18:00:00', '2026-11-21 21:00:00', 'PUBLISHED'),
    (4, 1, 'Piano Recital', 'Đêm độc tấu piano cổ điển.',
     '2026-12-05 19:00:00', '2026-12-05 21:00:00', 'PUBLISHED'),
    (5, 2, 'Cloud Engineering Workshop', 'Workshop thực hành về cloud và container.',
     '2026-12-12 09:00:00', '2026-12-12 16:00:00', 'PUBLISHED'),
    (6, 3, 'Technology Expo 2027', 'Triển lãm công nghệ và sản phẩm mới.',
     '2027-01-16 08:30:00', '2027-01-16 18:00:00', 'DRAFT');

INSERT INTO seats (id, venue_id, section_name, row_label, seat_number, seat_type) VALUES
    (1, 1, 'Tầng trệt', 'A', 1, 'VIP'),
    (2, 1, 'Tầng trệt', 'A', 2, 'VIP'),
    (3, 1, 'Tầng trệt', 'A', 3, 'VIP'),
    (4, 1, 'Tầng trệt', 'A', 4, 'VIP'),
    (5, 1, 'Tầng trệt', 'B', 1, 'STANDARD'),
    (6, 1, 'Tầng trệt', 'B', 2, 'STANDARD'),
    (7, 1, 'Tầng trệt', 'B', 3, 'STANDARD'),
    (8, 1, 'Tầng trệt', 'B', 4, 'STANDARD'),
    (9, 2, 'Hội trường', 'A', 1, 'VIP'),
    (10, 2, 'Hội trường', 'A', 2, 'VIP'),
    (11, 2, 'Hội trường', 'B', 1, 'STANDARD'),
    (12, 2, 'Hội trường', 'B', 2, 'STANDARD'),
    (13, 2, 'Hội trường', 'B', 3, 'STANDARD'),
    (14, 2, 'Hội trường', 'B', 4, 'STANDARD'),
    (15, 3, 'Khán đài A', 'A', 1, 'VIP'),
    (16, 3, 'Khán đài A', 'A', 2, 'VIP'),
    (17, 3, 'Khán đài A', 'B', 1, 'STANDARD'),
    (18, 3, 'Khán đài A', 'B', 2, 'STANDARD'),
    (19, 3, 'Khán đài B', 'C', 1, 'STANDARD'),
    (20, 3, 'Khán đài B', 'C', 2, 'STANDARD');

INSERT INTO event_seats (id, event_id, seat_id, price, status, version, held_until) VALUES
    (1, 1, 1, 1500000, 'BOOKED', 1, NULL),
    (2, 1, 2, 1500000, 'BOOKED', 1, NULL),
    (3, 1, 3, 1500000, 'AVAILABLE', 0, NULL),
    (4, 1, 4, 1500000, 'AVAILABLE', 0, NULL),
    (5, 1, 5, 1000000, 'BOOKED', 1, NULL),
    (6, 1, 6, 1000000, 'AVAILABLE', 0, NULL),
    (7, 1, 7, 1000000, 'AVAILABLE', 0, NULL),
    (8, 1, 8, 1000000, 'AVAILABLE', 0, NULL),
    (9, 2, 9, 600000, 'BOOKED', 1, NULL),
    (10, 2, 10, 600000, 'BOOKED', 1, NULL),
    (11, 2, 11, 350000, 'AVAILABLE', 0, NULL),
    (12, 2, 12, 350000, 'AVAILABLE', 0, NULL),
    (13, 2, 13, 350000, 'AVAILABLE', 0, NULL),
    (14, 2, 14, 350000, 'AVAILABLE', 0, NULL),
    (15, 3, 15, 700000, 'AVAILABLE', 0, NULL),
    (16, 3, 16, 700000, 'AVAILABLE', 0, NULL),
    (17, 3, 17, 400000, 'BOOKED', 1, NULL),
    (18, 3, 18, 400000, 'BOOKED', 1, NULL),
    (19, 3, 19, 400000, 'AVAILABLE', 0, NULL),
    (20, 3, 20, 400000, 'AVAILABLE', 0, NULL),
    (21, 4, 1, 1200000, 'HELD', 1, '2026-09-29 21:00:00'),
    (22, 4, 2, 1200000, 'AVAILABLE', 0, NULL),
    (23, 4, 3, 900000, 'AVAILABLE', 0, NULL),
    (24, 4, 4, 900000, 'AVAILABLE', 0, NULL),
    (25, 4, 5, 600000, 'AVAILABLE', 0, NULL),
    (26, 4, 6, 600000, 'AVAILABLE', 0, NULL),
    (27, 5, 9, 800000, 'AVAILABLE', 0, NULL),
    (28, 5, 10, 800000, 'AVAILABLE', 0, NULL),
    (29, 5, 11, 500000, 'AVAILABLE', 0, NULL),
    (30, 5, 12, 500000, 'AVAILABLE', 0, NULL),
    (31, 6, 15, 300000, 'AVAILABLE', 0, NULL),
    (32, 6, 16, 300000, 'AVAILABLE', 0, NULL),
    (33, 6, 17, 200000, 'AVAILABLE', 0, NULL),
    (34, 6, 18, 200000, 'AVAILABLE', 0, NULL);

INSERT INTO bookings (id, booking_code, user_id, event_id, status, total_amount, expires_at, created_at) VALUES
    (1, 'BKG-2026-0001', 2, 1, 'CONFIRMED', 3000000, NULL, '2026-09-20 09:15:00'),
    (2, 'BKG-2026-0002', 3, 1, 'CONFIRMED', 1000000, NULL, '2026-09-21 10:30:00'),
    (3, 'BKG-2026-0003', 4, 2, 'CONFIRMED', 1200000, NULL, '2026-09-22 14:00:00'),
    (4, 'BKG-2026-0004', 5, 3, 'CONFIRMED', 800000, NULL, '2026-09-23 19:20:00'),
    (5, 'BKG-2026-0005', 2, 4, 'PENDING', 1200000, '2026-09-29 21:00:00', '2026-09-29 20:45:00'),
    (6, 'BKG-2026-0006', 6, 5, 'CANCELLED', 500000, NULL, '2026-09-25 08:10:00');

INSERT INTO booking_items (id, booking_id, event_seat_id, unit_price) VALUES
    (1, 1, 1, 1500000),
    (2, 1, 2, 1500000),
    (3, 2, 5, 1000000),
    (4, 3, 9, 600000),
    (5, 3, 10, 600000),
    (6, 4, 17, 400000),
    (7, 4, 18, 400000),
    (8, 5, 21, 1200000),
    (9, 6, 29, 500000);

INSERT INTO payments (id, booking_id, transaction_code, payment_method, amount, status, paid_at) VALUES
    (1, 1, 'TXN-2026-0001', 'E_WALLET', 3000000, 'SUCCESS', '2026-09-20 09:17:00'),
    (2, 2, 'TXN-2026-0002', 'CARD', 1000000, 'SUCCESS', '2026-09-21 10:32:00'),
    (3, 3, 'TXN-2026-0003', 'BANK_TRANSFER', 1200000, 'SUCCESS', '2026-09-22 14:05:00'),
    (4, 4, 'TXN-2026-0004', 'E_WALLET', 800000, 'SUCCESS', '2026-09-23 19:22:00'),
    (5, 5, NULL, 'BANK_TRANSFER', 1200000, 'PENDING', NULL),
    (6, 6, 'TXN-2026-0006', 'CARD', 500000, 'REFUNDED', '2026-09-25 08:15:00');

-- =============================================================
-- 3. QUERY 
-- =============================================================

-- 3.1. Danh sách booking một user có id là 2
    SELECT * FROM bookings WHERE user_id = 2;

-- 3.2. Ghế còn trống của sự kiện có id là 1
    SELECT * FROM event_seats WHERE event_id = 1 AND status = 'AVAILABLE';

-- 3.3. Chi tiết booking gồm user, event và ghế.
    SELECT u.id, u.full_name, e.name, es.seat_id 
    FROM users u 
    JOIN bookings b ON u.id = b.user_id 
    JOIN events e ON b.event_id = e.id 
    JOIN booking_items bi ON b.id = bi.booking_id 
    JOIN event_seats es ON bi.event_seat_id = es.id;

-- 3.4. Tổng doanh thu theo sự kiện.
    SELECT e.id, e.name, sum(b.total_amount ) as revenue
    FROM events e
    JOIN bookings b on b.event_id = e.id 
    AND b.status = 'CONFIRMED'
    GROUP BY e.id;

-- 3.5. Số vé bán được theo từng sự kiện.
-- Mỗi booking_item tương ứng với một vé. Chỉ tính booking CONFIRMED.
    SELECT e.id, e.name, COUNT(bi.id) AS tickets_sold
    FROM events e
    LEFT JOIN bookings b
        ON b.event_id = e.id
        AND b.status = 'CONFIRMED'
    LEFT JOIN booking_items bi
        ON bi.booking_id = b.id
    GROUP BY e.id, e.name
    ORDER BY e.id;

-- 3.6. Những sự kiện có doanh thu lớn hơn một giá trị nhất định.
	SET @min_revenue = 1000000;

    SELECT e.id, e.name, COALESCE(SUM(b.total_amount), 0) AS revenue
    FROM events e
    LEFT JOIN bookings b
        ON b.event_id = e.id
        AND b.status = 'CONFIRMED'
    GROUP BY e.id, e.name
    HAVING revenue > @min_revenue
    ORDER BY revenue DESC;

-- 3.7. User đặt nhiều vé nhất.
    SELECT u.id, u.full_name, u.email, COUNT(bi.id) AS tickets_booked
    FROM users u
    JOIN bookings b
        ON b.user_id = u.id
        AND b.status = 'CONFIRMED'
    JOIN booking_items bi
        ON bi.booking_id = b.id
    GROUP BY u.id, u.full_name, u.email
    ORDER BY tickets_booked DESC, u.id ASC
    LIMIT 1;

-- 3.8. Những event chưa có booking nào.
    SELECT e.id, e.name, e.start_at, e.status
    FROM events e
    LEFT JOIN bookings b
        ON b.event_id = e.id
    WHERE b.id IS NULL
    ORDER BY e.start_at;

-- 3.9. Sắp xếp event theo doanh thu từ cao xuống thấp.
-- LEFT JOIN giúp các event chưa có doanh thu vẫn xuất hiện với revenue = 0.
    SELECT e.id, e.name, COALESCE(SUM(b.total_amount), 0) AS revenue
    FROM events e
    LEFT JOIN bookings b
        ON b.event_id = e.id
        AND b.status = 'CONFIRMED'
    GROUP BY e.id, e.name
    ORDER BY revenue DESC, e.id ASC;

-- 3.10. Thống kê số ghế và giá vé theo từng sự kiện.
    SELECT e.id, e.name, 
        COUNT(es.id) AS total_seats, 
        MIN(es.price) AS min_price, 
        MAX(es.price) AS max_price, 
        ROUND(AVG(es.price), 2) AS average_price
    FROM events e
    LEFT JOIN event_seats es
        ON es.event_id = e.id
    GROUP BY e.id, e.name
    ORDER BY e.id;




