package com.booksharing.repository;

import com.booksharing.entity.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByTargetId(UUID targetId);

    List<Review> findByExchangeId(UUID exchangeId);

    boolean existsByExchangeIdAndAuthorId(UUID exchangeId, UUID authorId);

    @Query("select avg(r.rating) from Review r where r.target.id = :targetId")
    Double averageRatingByTargetId(@Param("targetId") UUID targetId);
}