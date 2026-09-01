package com.booksharing.model.listing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingPhotoRepository extends JpaRepository<ListingPhoto, UUID> {

    List<ListingPhoto> findByListingId(UUID listingId);

    void deleteByListingIdAndId(UUID listingId, UUID photoId);
}
