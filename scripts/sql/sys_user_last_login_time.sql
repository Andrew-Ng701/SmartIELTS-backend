ALTER TABLE sys_user
    ADD COLUMN IF NOT EXISTS last_login_time DATETIME NULL AFTER created_time;
