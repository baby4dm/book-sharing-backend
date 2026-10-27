package com.booksharing.repository;

import com.booksharing.entity.ModerationAction;
import com.booksharing.enums.ModerationTargetType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {

    @EntityGraph(attributePaths = {"moderator", "targetUser"})
    Page<ModerationAction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"moderator", "targetUser"})
    Page<ModerationAction> findByTargetTypeInOrderByCreatedAtDesc(
            Collection<ModerationTargetType> targetTypes, Pageable pageable);

    @EntityGraph(attributePaths = {"moderator", "targetUser"})
    List<ModerationAction> findTop30ByTargetUserIdOrderByCreatedAtDesc(UUID targetUserId);
}
