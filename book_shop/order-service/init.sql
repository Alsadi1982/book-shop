-- Создание таблицы заказов
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(50) UNIQUE NOT NULL,
    total_amount DECIMAL(10, 2),
    status VARCHAR(20) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    shipping_address TEXT,
    payment_method VARCHAR(50)
    );

-- Создание таблицы позиций заказа
CREATE TABLE IF NOT EXISTS order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    book_title VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    subtotal DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
    );

INSERT INTO orders (user_id, order_number, total_amount, status, order_date, shipping_address, payment_method)
VALUES (
           1,
           'ORD-20241201-001',
           1199.98,
           'COMPLETED',
           '2024-12-01 10:30:00',
           'г. Москва, ул. Тверская, д. 15, кв. 45',
           'CREDIT_CARD'
       );

INSERT INTO order_items (order_id, book_id, book_title, quantity, price, subtotal)
VALUES
    (1, 101, 'Мастер и Маргарита', 2, 599.99, 1199.98);

INSERT INTO orders (user_id, order_number, total_amount, status, order_date, shipping_address, payment_method)
VALUES (
           2,
           'ORD-20241215-002',
           2740.00,
           'PROCESSING',
           '2024-12-15 14:20:00',
           'г. Санкт-Петербург, Невский пр., д. 25, кв. 12',
           'PAYPAL'
       );

INSERT INTO order_items (order_id, book_id, book_title, quantity, price, subtotal)
VALUES
    (2, 102, 'Clean Code: Идеальный программный код', 1, 1200.00, 1200.00),
    (2, 103, 'Sapiens. Краткая история человечества', 2, 770.00, 1540.00);

INSERT INTO orders (user_id, order_number, total_amount, status, order_date, shipping_address, payment_method)
VALUES (
           1,
           'ORD-20241220-003',
           3520.00,
           'PENDING',
           '2024-12-20 09:15:00',
           'г. Москва, ул. Тверская, д. 15, кв. 45',
           'CREDIT_CARD'
       );

INSERT INTO order_items (order_id, book_id, book_title, quantity, price, subtotal)
VALUES
    (3, 104, 'Преступление и наказание', 1, 450.00, 450.00),
    (3, 105, 'Java. Полное руководство', 1, 1500.00, 1500.00),
    (3, 106, 'Краткая история времени', 1, 890.00, 890.00),
    (3, 107, 'От нуля к единице', 1, 680.00, 680.00);

INSERT INTO orders (user_id, order_number, total_amount, status, order_date, shipping_address, payment_method)
VALUES (
           3,
           'ORD-20241225-004',
           980.00,
           'CANCELLED',
           '2024-12-25 16:45:00',
           'г. Новосибирск, Красный пр., д. 8, кв. 67',
           'CREDIT_CARD'
       );

INSERT INTO order_items (order_id, book_id, book_title, quantity, price, subtotal)
VALUES
    (4, 108, 'Думай медленно... решай быстро', 1, 980.00, 980.00);

INSERT INTO orders (user_id, order_number, total_amount, status, order_date, shipping_address, payment_method)
VALUES (
           2,
           'ORD-20241228-005',
           1200.00,
           'COMPLETED',
           '2024-12-28 11:00:00',
           'г. Санкт-Петербург, Невский пр., д. 25, кв. 12',
           'PAYPAL'
       );

INSERT INTO order_items (order_id, book_id, book_title, quantity, price, subtotal)
VALUES
    (5, 102, 'Clean Code: Идеальный программный код', 1, 1200.00, 1200.00);