-- Previous releases persisted raw credentials and configuration values in operation logs.
-- Retain the audit event while removing values that must never remain at rest.
UPDATE sys_operation_log
SET parameters = '[REDACTED: legacy sensitive operation parameters]',
    result = NULL
WHERE description IN (
    'User Registration',
    'User Login',
    'Send Login OTP',
    'OTP Login',
    'Change Password',
    'Create Webhook',
    'Update Webhook',
    'Create Config',
    'Update Config'
);
