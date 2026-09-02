package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByTargetId(UUID targetId);

    List<Review> findByExchangeId(UUID exchangeId);
}
