package com.booksharing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.booksharing.enums.ShipmentDirection;
import com.booksharing.entity.ShipmentInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentInfoRepository extends JpaRepository<ShipmentInfo, UUID> {

    List<ShipmentInfo> findByExchangeId(UUID exchangeId);

    Optional<ShipmentInfo> findByExchangeIdAndDirection(UUID exchangeId, ShipmentDirection direction);
}
