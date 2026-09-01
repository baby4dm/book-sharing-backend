package com.booksharing.model.exchange;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadlineExtensionRequestRepository extends JpaRepository<DeadlineExtensionRequest, UUID> {

    List<DeadlineExtensionRequest> findByExchangeId(UUID exchangeId);

    List<DeadlineExtensionRequest> findByExchangeIdAndStatus(UUID exchangeId, ExtensionStatus status);
}
