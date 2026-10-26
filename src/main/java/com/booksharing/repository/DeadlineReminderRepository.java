package com.booksharing.repository;

import com.booksharing.entity.Exchange;
import com.booksharing.enums.ExchangeStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DeadlineReminderRepository extends Repository<Exchange, UUID> {

    @Query("""
            select e from Exchange e
              join fetch e.owner
              join fetch e.reader
              join fetch e.listing l
              join fetch l.bookCatalogEntry
            where e.status in :statuses
              and e.deadline is not null
              and e.returnedAt is null
            """)
    List<Exchange> findActiveReadings(@Param("statuses") Collection<ExchangeStatus> statuses);

    @Modifying
    @Query(value = """
            insert into deadline_reminders (exchange_id, kind, due_date)
            values (:exchangeId, :kind, :dueDate)
            on conflict do nothing
            """, nativeQuery = true)
    int markSent(@Param("exchangeId") UUID exchangeId,
                 @Param("kind") String kind,
                 @Param("dueDate") LocalDate dueDate);
}