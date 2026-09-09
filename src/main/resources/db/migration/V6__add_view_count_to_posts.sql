ALTER TABLE posts
    ADD COLUMN view_count BIGINT NOT NULL DEFAULT 0,
    ADD CONSTRAINT chk_posts_view_count_not_negative CHECK (view_count >= 0);