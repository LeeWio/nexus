-- Retain the earliest row if historical concurrent writes produced duplicates.
DELETE duplicate_revision
FROM blog_post_revision duplicate_revision
JOIN blog_post_revision retained_revision
  ON retained_revision.post_id = duplicate_revision.post_id
 AND retained_revision.version_number = duplicate_revision.version_number
 AND retained_revision.id < duplicate_revision.id;

ALTER TABLE blog_post_revision
    ADD CONSTRAINT uk_revision_post_version UNIQUE (post_id, version_number);
