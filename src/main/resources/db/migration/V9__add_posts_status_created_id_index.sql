CREATE INDEX idx_posts_status_created_id ON posts (status, created_at DESC, id DESC);
