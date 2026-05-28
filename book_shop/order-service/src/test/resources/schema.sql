DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;

CREATE TABLE IF NOT EXISTS orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_number VARCHAR(255) UNIQUE NOT NULL,
    total_amount DECIMAL(19, 2),
    status VARCHAR(255),
    order_date TIMESTAMP,
    shipping_address VARCHAR(500),
    payment_method VARCHAR(255)
);

CREATE TABLE order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT,
    book_id BIGINT,
    book_title VARCHAR(255),
    quantity INTEGER,
    price DECIMAL(19, 2),
    subtotal DECIMAL(19, 2)
);