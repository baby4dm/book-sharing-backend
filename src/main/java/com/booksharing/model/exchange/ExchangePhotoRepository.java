package com.booksharing.model.exchange;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangePhotoRepository extends JpaRepository<ExchangePhoto, UUID> {

    List<ExchangePhoto> findByExchangeId(UUID exchangeId);

    List<ExchangePhoto> findByExchangeIdAndStage(UUID exchangeId, PhotoStage stage);
}
