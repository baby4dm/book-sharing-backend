package com.booksharing.repository;

import com.booksharing.entity.PostLike;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostLikeRepository extends JpaRepository<PostLike, UUID> {

    boolean existsByPostIdAndUserId(UUID postId, UUID userId);

    @Modifying
    @Query(value = """
            insert into post_likes (id, post_id, user_id, created_at)
            values (gen_random_uuid(), :postId, :userId, now())
            on conflict (post_id, user_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(@Param("postId") UUID postId, @Param("userId") UUID userId);

    long countByPostId(UUID postId);

    @Modifying
    @Query("delete from PostLike l where l.post.id = :postId and l.user.id = :userId")
    int deleteByPostIdAndUserId(@Param("postId") UUID postId, @Param("userId") UUID userId);

    @Query("select l.post.id, count(l) from PostLike l where l.post.id in :postIds group by l.post.id")
    List<Object[]> countByPostIds(@Param("postIds") Collection<UUID> postIds);

    @Query("select l.post.id from PostLike l where l.user.id = :userId and l.post.id in :postIds")
    List<UUID> findLikedPostIds(@Param("userId") UUID userId, @Param("postIds") Collection<UUID> postIds);
}