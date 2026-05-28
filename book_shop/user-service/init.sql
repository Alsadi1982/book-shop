CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    );

INSERT INTO users (username, password, email, first_name, last_name, phone, role, active, created_at, updated_at)
VALUES ('ivan.petrov', 'password123', 'ivan.petrov@example.com', 'Иван', 'Петров', '+7-916-123-4567', 'CUSTOMER', true,
        '2024-01-15 10:30:00', '2024-01-15 10:30:00'),
       ('maria.sidorova', 'password123', 'maria.sidorova@example.com', 'Мария', 'Сидорова', '+7-916-234-5678',
        'CUSTOMER', true, '2024-02-20 14:15:00', '2024-02-20 14:15:00'),
       ('alexey.ivanov', 'password123', 'alexey.ivanov@example.com', 'Алексей', 'Иванов', '+7-916-345-6789', 'CUSTOMER',
        true, '2024-03-10 09:45:00', '2024-02-20 14:15:00'),
       ('ekaterina.smirnova', 'password123', 'ekaterina.smirnova@example.com', 'Екатерина', 'Смирнова',
        '+7-916-456-7890', 'CUSTOMER', true, '2024-04-05 16:20:00', '2024-04-05 16:20:00'),
       ('admin', 'admin123', 'admin@bookstore.com', 'Администратор', 'Системы', '+7-916-567-8901', 'ADMIN', true,
        '2024-01-01 00:00:00', '2024-01-01 00:00:00');