-- 1. Создаем таблицу пользователей
CREATE TABLE users (
       id BIGSERIAL PRIMARY KEY,
       username VARCHAR(50) NOT NULL UNIQUE,
       password VARCHAR(120) NOT NULL,
       email VARCHAR(100) NOT NULL UNIQUE,
       hourly_rate NUMERIC(8, 2) NOT NULL,
       role VARCHAR(20) NOT NULL DEFAULT 'USER',
       is_enabled BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. Создаем таблицу рабочих смен
CREATE TABLE work_shifts (
       id BIGSERIAL PRIMARY KEY,
       user_id BIGINT NOT NULL,
       start_time TIMESTAMP NOT NULL,
       end_time TIMESTAMP,
       break_duration_minutes INTEGER DEFAULT 0,
       rate_at_the_time NUMERIC(8, 2),
       profit NUMERIC(12, 2) DEFAULT 0.00,
       CONSTRAINT fk_work_shifts_user FOREIGN KEY (user_id) REFERENCES users(id)
);