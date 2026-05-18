-- V21: Update FriendLink and Menu for dynamic navigation and applications

-- Update blog_friend_link
ALTER TABLE `blog_friend_link` 
    ADD COLUMN `email` VARCHAR(100) AFTER `description`,
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'APPROVED' AFTER `email`;

-- Update sys_menu
ALTER TABLE `sys_menu`
    ADD COLUMN `is_visible` BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN `is_public` BOOLEAN NOT NULL DEFAULT FALSE;

-- Ensure is_deleted exists on sys_menu (might have been missed in V18 check)
-- Actually I manually checked sys_menu in V18 logic, let's verify.
-- I'll add an ignore-if-exists style check or just add it if needed.
-- V18 already had it for sys_menu.
