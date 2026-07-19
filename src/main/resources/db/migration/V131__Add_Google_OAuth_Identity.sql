ALTER TABLE sys_user
    ADD COLUMN google_id VARCHAR(100) NULL AFTER github_username;

CREATE UNIQUE INDEX uk_user_google_id ON sys_user(google_id);
