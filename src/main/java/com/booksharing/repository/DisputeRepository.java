package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Dispute;
import com.booksharing.enums.DisputeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByStatus(DisputeStatus status);

    List<Dispute> findByExchangeId(UUID exchangeId);
}
