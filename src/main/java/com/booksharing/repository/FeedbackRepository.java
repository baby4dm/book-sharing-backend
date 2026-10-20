package com.booksharing.repository;

import com.booksharing.entity.Feedback;
import com.booksharing.entity.User;
import com.booksharing.enums.FeedbackStatus;
import com.booksharing.enums.FeedbackTopic;
import com.booksharing.enums.UserRole;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    long countByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    @Query("select count(f) from Feedback f where lower(f.email) = lower(:email) and f.createdAt > :after")
    long countByEmailSince(@Param("email") String email, @Param("after") LocalDateTime after);

    @EntityGraph(attributePaths = {"user", "handledBy"})
    @Query("""
            select f from Feedback f
            where (:status is null or f.status = :status)
              and (:topic is null or f.topic = :topic)
            order by f.createdAt desc
            """)
    Page<Feedback> search(
            @Param("status") FeedbackStatus status,
            @Param("topic") FeedbackTopic topic,
            Pageable pageable);

    long countByStatus(FeedbackStatus status);

    @Query("select u from User u where u.role in :roles")
    List<User> findUsersByRoles(@Param("roles") Collection<UserRole> roles);
}