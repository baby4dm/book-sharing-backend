package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Dispute;
import com.booksharing.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByStatus(DisputeStatus status);

    List<Dispute> findByExchangeId(UUID exchangeId);

    List<Dispute> findByStatusOrderByCreatedAtAsc(DisputeStatus status);

    List<Dispute> findTop100ByStatusNotOrderByResolvedAtDesc(DisputeStatus status);

    long countByStatus(DisputeStatus status);

    long countByFiledById(UUID userId);

    @Query("""
            select count(d) from Dispute d
            where d.filedBy.id <> :userId
              and (d.exchange.owner.id = :userId or d.exchange.reader.id = :userId)
            """)
    long countAgainstUser(@Param("userId") UUID userId);
}
