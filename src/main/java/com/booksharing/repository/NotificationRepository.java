package com.booksharing.repository;

import com.booksharing.entity.Notification;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    List<Notification> findByUserIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(
            UUID userId, LocalDateTime before, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDescIdDesc(UUID userId, Pageable pageable);

    List<Notification> findByUserIdAndIsReadFalseAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(
            UUID userId, LocalDateTime before, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    @EntityGraph(attributePaths = "user")
    Optional<Notification> findWithUserById(UUID id);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Notification n set n.isRead = true where n.user.id = :userId and n.isRead = false")
    int markAllRead(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from Notification n where n.isRead = true and n.createdAt < :before")
    int deleteReadOlderThan(@Param("before") LocalDateTime before);

    @Query("select e.request.id, e.id from Exchange e where e.request.id in :requestIds")
    List<Object[]> findExchangeIdsByRequestIds(@Param("requestIds") Collection<UUID> requestIds);

    @Query("select d.id, d.exchange.id from Dispute d where d.id in :disputeIds")
    List<Object[]> findExchangeIdsByDisputeIds(@Param("disputeIds") Collection<UUID> disputeIds);
}