package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.ListingPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, UUID> {

    List<ListingPhoto> findByListingId(UUID listingId);

    void deleteByListingIdAndId(UUID listingId, UUID photoId);
}
