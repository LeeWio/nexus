-- V17: Add unique constraint to sys_user.email and improve user indexes

ALTER TABLE `sys_user` ADD UNIQUE INDEX `idx_user_email` (`email`);
CREATE INDEX `idx_user_status` ON `sys_user` (`status`);
