-- Reading history stores the latest position independently from the furthest
-- progress reached. Existing values seed both dimensions for the first rollout.
ALTER TABLE blog_reading_history
    ADD COLUMN current_progress_percent INT NOT NULL DEFAULT 0 AFTER progress_percent,
    ADD COLUMN last_observed_at DATETIME(3) NULL AFTER last_read_at;

UPDATE blog_reading_history
SET current_progress_percent = progress_percent,
    last_observed_at = last_read_at
WHERE last_observed_at IS NULL;

ALTER TABLE blog_reading_history
    MODIFY COLUMN last_observed_at DATETIME(3) NOT NULL,
    ADD KEY idx_reading_history_user_observed (user_id, last_observed_at DESC);
