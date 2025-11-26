CREATE TABLE IF NOT EXISTS tasks (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     title VARCHAR(255),
    description VARCHAR(1000),
    status VARCHAR(50),
    priority INT,
    created_at TIMESTAMP
    );