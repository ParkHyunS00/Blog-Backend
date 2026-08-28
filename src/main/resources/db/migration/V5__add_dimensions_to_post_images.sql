ALTER TABLE post_images
    ADD COLUMN width INT NOT NULL,
    ADD COLUMN height INT NOT NULL,
    ADD CONSTRAINT chk_post_images_width_positive CHECK (width > 0),
    ADD CONSTRAINT chk_post_images_height_positive CHECK (height > 0);