-- Native notification inserts omit id. Hibernate IDENTITY only auto-assigns ids
-- through entity persistence, so these columns must be real AUTO_INCREMENT keys.
-- MySQL refuses to change a referenced primary key while the foreign key exists.
ALTER TABLE sys_notification_delivery
    DROP FOREIGN KEY fk_notification_delivery_notification;

ALTER TABLE sys_notification
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT;

ALTER TABLE sys_notification_delivery
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT,
    ADD CONSTRAINT fk_notification_delivery_notification
        FOREIGN KEY (notification_id) REFERENCES sys_notification (id) ON DELETE CASCADE;
