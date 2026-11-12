
BEGIN;

DELETE FROM users WHERE verified = false;

ALTER TABLE users
    DROP COLUMN verified,
    DROP COLUMN device_fingerprint,
    DROP COLUMN last_login_ip;

UPDATE users SET email = lower(email) WHERE email <> lower(email);

COMMIT;
