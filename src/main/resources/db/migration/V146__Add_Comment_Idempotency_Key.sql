-- Let clients safely retry comment and guestbook submissions without creating duplicates.
ALTER TABLE blog_comment
    ADD COLUMN client_request_id VARCHAR(80) NULL;

CREATE UNIQUE INDEX uk_comment_user_client_request
    ON blog_comment (user_id, client_request_id);
