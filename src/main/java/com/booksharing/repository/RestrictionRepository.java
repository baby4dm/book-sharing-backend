package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Restriction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestrictionRepository extends JpaRepository<Restriction, UUID> {

    List<Restriction> findByUserId(UUID userId);
}
