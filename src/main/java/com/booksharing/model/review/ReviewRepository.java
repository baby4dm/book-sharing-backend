package com.booksharing.model.review;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    List<Review> findByTargetId(UUID targetId);

    List<Review> findByExchangeId(UUID exchangeId);
}
