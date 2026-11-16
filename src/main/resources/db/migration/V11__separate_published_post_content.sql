ALTER TABLE posts
    MODIFY COLUMN title VARCHAR(200) NULL,
    MODIFY COLUMN content LONGTEXT NULL,
    ADD COLUMN published_title VARCHAR(200) NULL,
    ADD COLUMN published_content LONGTEXT NULL;

UPDATE posts
SET published_title = title,
    published_content = content,
    title = NULL,
    content = NULL
WHERE status = 'PUBLISHED';

ALTER TABLE posts
    DROP INDEX ft_posts_title,
    DROP INDEX ft_posts_content;

CREATE FULLTEXT INDEX ft_posts_title_content
ON posts (published_title, published_content)
WITH PARSER ngram;

ALTER TABLE posts
    ADD CONSTRAINT chk_posts_content_by_status CHECK (
        (status = 'DRAFT' AND published_title IS NULL AND published_content IS NULL)
        OR
        (status = 'PUBLISHED' AND title IS NULL AND content IS NULL
            AND published_title IS NOT NULL AND published_content IS NOT NULL)
    );
