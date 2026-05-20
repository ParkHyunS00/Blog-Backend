CREATE TABLE admin_auth_attempt (
    id INT NOT NULL PRIMARY KEY,
    otp_fail_count INT NOT NULL DEFAULT 0,
    locked BIT(1) NOT NULL DEFAULT 0,
    last_failed_at DATETIME(6) NULL
);