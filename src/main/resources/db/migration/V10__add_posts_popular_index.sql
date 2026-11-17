CREATE INDEX idx_posts_popular
ON posts (view_count, created_at);
