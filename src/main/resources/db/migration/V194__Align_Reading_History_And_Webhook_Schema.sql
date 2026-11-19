-- Bring Flyway-only schemas in line with the mappings used by the application.
-- Legacy schemas previously managed by Hibernate may already have these changes.
SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'blog_reading_history'
          AND column_name = 'progress_percent' AND data_type = 'tinyint'),
    'ALTER TABLE blog_reading_history MODIFY COLUMN progress_percent INT NOT NULL DEFAULT 0',
    'DO 0'
);
PREPARE nexus_statement FROM @ddl;
EXECUTE nexus_statement;
DEALLOCATE PREPARE nexus_statement;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'blog_webhook' AND column_name = 'created_by'),
    'DO 0',
    'ALTER TABLE blog_webhook ADD COLUMN created_by VARCHAR(255) NULL'
);
PREPARE nexus_statement FROM @ddl;
EXECUTE nexus_statement;
DEALLOCATE PREPARE nexus_statement;

SET @ddl = IF(
    EXISTS (SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = 'blog_webhook' AND column_name = 'last_modified_by'),
    'DO 0',
    'ALTER TABLE blog_webhook ADD COLUMN last_modified_by VARCHAR(255) NULL'
);
PREPARE nexus_statement FROM @ddl;
EXECUTE nexus_statement;
DEALLOCATE PREPARE nexus_statement;
