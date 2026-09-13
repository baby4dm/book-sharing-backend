package com.booksharing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.booksharing.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByGoogleId(String googleId);

    boolean existsByEmail(String email);

    /**
     * Native SQL, не JPQL - об'єднує (UNION) населені пункти з профілів
     * users і з override-полів listings.settlement_name, інакше список
     * "усі доступні міста для фільтра" пропустив би оголошення, де
     * власник вказав ІНШЕ місце, ніж у своєму профілі.
     */
    @Query(value = "SELECT DISTINCT settlement_name FROM ("
            + "SELECT settlement_name FROM users WHERE settlement_name IS NOT NULL "
            + "UNION "
            + "SELECT settlement_name FROM listings WHERE settlement_name IS NOT NULL"
            + ") t ORDER BY settlement_name", nativeQuery = true)
    List<String> findDistinctCities();
}