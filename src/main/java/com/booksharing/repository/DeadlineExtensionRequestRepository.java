package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.DeadlineExtensionRequest;
import com.booksharing.enums.ExtensionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeadlineExtensionRequestRepository extends JpaRepository<DeadlineExtensionRequest, UUID> {

    List<DeadlineExtensionRequest> findByExchangeId(UUID exchangeId);

    List<DeadlineExtensionRequest> findByExchangeIdAndStatus(UUID exchangeId, ExtensionStatus status);
}
