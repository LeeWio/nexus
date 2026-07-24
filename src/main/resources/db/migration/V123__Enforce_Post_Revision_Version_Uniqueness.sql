-- Retain the earliest row if historical concurrent writes produced duplicates.
DELETE FROM blog_post_revision
WHERE id IN (
    SELECT duplicate_id FROM (
        SELECT d.id AS duplicate_id
        FROM blog_post_revision d
        JOIN blog_post_revision r
          ON r.post_id = d.post_id
         AND r.version_number = d.version_number
         AND r.id < d.id
    ) t
);

ALTER TABLE blog_post_revision
    ADD CONSTRAINT uk_revision_post_version UNIQUE (post_id, version_number);
