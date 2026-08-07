ALTER TABLE work_shifts
ADD COLUMN current_break_start_time TIMESTAMP DEFAULT NULL;
-- 2. Создаем новую таблицу для учета больничных, отпусков и отгулов
CREATE TABLE absence_records (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    absence_type VARCHAR(30) NOT NULL, -- Сюда будем писать VACATION, SICK_LEAVE, DAY_OFF
    reason VARCHAR(255),               -- Комментарий (например, "По семейным обстоятельствам")
    approved BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_absence_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индекс для быстрого поиска пропусков конкретного сотрудника за выбранный месяц
CREATE INDEX idx_absence_user_dates ON absence_records(user_id, start_date, end_date);