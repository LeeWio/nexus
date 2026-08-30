ALTER TABLE blog_newsletter_delivery
    ADD COLUMN recipient VARCHAR(255) NULL,
    ADD COLUMN subject VARCHAR(255) NULL,
    ADD COLUMN template_name VARCHAR(255) NULL,
    ADD COLUMN template_variables TEXT NULL;
