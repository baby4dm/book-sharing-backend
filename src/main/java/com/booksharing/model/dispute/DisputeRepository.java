package com.booksharing.model.dispute;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByStatus(DisputeStatus status);

    List<Dispute> findByExchangeId(UUID exchangeId);
}
