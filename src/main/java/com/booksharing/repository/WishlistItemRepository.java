package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByUserId(UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
