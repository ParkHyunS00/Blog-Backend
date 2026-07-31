CREATE TABLE categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT uk_categories_name UNIQUE (name),
    CONSTRAINT uk_categories_slug UNIQUE (slug)
);

CREATE TABLE posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    content LONGTEXT NOT NULL,
    category_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_posts_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT chk_posts_status CHECK (status IN ('DRAFT', 'PUBLISHED'))
);

CREATE TABLE post_images (
     id BIGINT AUTO_INCREMENT PRIMARY KEY,
     post_id BIGINT NULL,
     type VARCHAR(20) NOT NULL,
     object_key VARCHAR(500) NOT NULL,
     mime_type VARCHAR(100) NOT NULL,
     created_at DATETIME NOT NULL,
     updated_at DATETIME NOT NULL,

     CONSTRAINT fk_post_images_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
     CONSTRAINT chk_post_images_type CHECK (type IN ('THUMBNAIL', 'CONTENT')),
     CONSTRAINT uk_post_images_object_key UNIQUE (object_key)
);

CREATE TABLE tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(20) NOT NULL,
    slug VARCHAR(80) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT uk_tags_name UNIQUE (name),
    CONSTRAINT uk_tags_slug UNIQUE (slug)
);

CREATE TABLE post_tags (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,

    CONSTRAINT fk_post_tags_post FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE,
    CONSTRAINT fk_post_tags_tag FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE,
    CONSTRAINT uk_post_tags_post_tag UNIQUE (post_id, tag_id)
);