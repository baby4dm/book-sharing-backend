package com.booksharing.model.feed;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, UUID> {

    // сторінкова видача для стрічки, найновіші перші
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
