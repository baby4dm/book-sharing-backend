package com.booksharing.repository;

import com.booksharing.entity.ChatRoom;
import com.booksharing.entity.Exchange;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    List<ChatRoom> findByUserAIdOrUserBId(UUID userAId, UUID userBId);

    @Query("""
            select r from ChatRoom r
            where (r.userA.id = :first and r.userB.id = :second)
               or (r.userA.id = :second and r.userB.id = :first)
            """)
    Optional<ChatRoom> findBetween(@Param("first") UUID first, @Param("second") UUID second);

    @Query("""
            select r from ChatRoom r
              join fetch r.userA
              join fetch r.userB
            where r.userA.id = :userId or r.userB.id = :userId
            """)
    List<ChatRoom> findAllForUser(@Param("userId") UUID userId);

    @Query("""
            select r from ChatRoom r
              join fetch r.userA
              join fetch r.userB
            where r.id = :id
            """)
    Optional<ChatRoom> findWithDetailsById(@Param("id") UUID id);


    @Query("""
            select e from Exchange e
              join fetch e.listing l
              join fetch l.bookCatalogEntry
            where e.owner.id = :userId or e.reader.id = :userId
            order by e.createdAt desc
            """)
    List<Exchange> findExchangesOfUser(@Param("userId") UUID userId);
}