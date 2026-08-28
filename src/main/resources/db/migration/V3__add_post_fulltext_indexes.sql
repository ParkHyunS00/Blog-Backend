ALTER TABLE posts ADD FULLTEXT INDEX ft_posts_title (title) WITH PARSER ngram;

ALTER TABLE posts ADD FULLTEXT INDEX ft_posts_content (content) WITH PARSER ngram;