ALTER TABLE sys_file 
    ADD COLUMN file_hash VARCHAR(64) DEFAULT NULL COMMENT 'SHA-256 hash of file content',
    ADD COLUMN reference_count INT NOT NULL DEFAULT 1 COMMENT 'Number of entities referencing this physical file';

CREATE INDEX idx_file_hash ON sys_file(file_hash);
