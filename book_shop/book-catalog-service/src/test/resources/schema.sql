DROP TABLE IF EXISTS books;
DROP TABLE IF EXISTS categories;

CREATE TABLE IF NOT EXISTS books (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isbn VARCHAR(255) UNIQUE NOT NULL,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255),
    description VARCHAR(255),
    price DECIMAL(19, 2),
    stock_quantity INTEGER,
    category_id BIGINT,
    publisher_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);