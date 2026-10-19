-- Вподобання постів у стрічці «Спільнота».
-- Один користувач може вподобати пост лише раз (UNIQUE),
-- при видаленні поста чи користувача вподобання видаляються разом із ним.
CREATE TABLE IF NOT EXISTS post_likes (
                                          id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id    UUID      NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id    UUID      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_post_likes_post_user UNIQUE (post_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_post_likes_post_id ON post_likes(post_id);
CREATE INDEX IF NOT EXISTS idx_posts_created_at ON posts(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comments_post_id ON comments(post_id);