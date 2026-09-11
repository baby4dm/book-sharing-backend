package com.booksharing.mapper;

import java.util.List;

import com.booksharing.entity.Listing;
import com.booksharing.dto.res.ListingResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * {@code Listing} не має навігаційної колекції на {@code ListingPhoto}
 * (свідомо - щоб уникнути проблем з lazy-колекціями й N+1 у простих
 * випадках, коли фото не потрібні). Тому {@code toResponse} приймає
 * {@code photoUrls} другим параметром - MapStruct мапить його на
 * однойменне поле DTO автоматично, без явної {@code @Mapping}.
 */
@Mapper(componentModel = "spring")
public interface ListingMapper {

    @Mapping(target = "ownerId", source = "listing.owner.id")
    @Mapping(target = "ownerName", source = "listing.owner.name")
    @Mapping(target = "ownerAvatarUrl", source = "listing.owner.avatarUrl")
    @Mapping(target = "ownerCity", source = "listing.owner.city")
    @Mapping(target = "ownerRatingAvg", source = "listing.owner.ratingAvg")
    @Mapping(target = "bookCatalogEntryId", source = "listing.bookCatalogEntry.id")
    @Mapping(target = "bookTitle", source = "listing.bookCatalogEntry.title")
    @Mapping(target = "bookAuthor", source = "listing.bookCatalogEntry.author")
    @Mapping(target = "bookCoverUrl", source = "listing.bookCatalogEntry.coverUrl")
    @Mapping(target = "bookGenre", source = "listing.bookCatalogEntry.genre")
    ListingResponse toResponse(Listing listing, List<String> photoUrls);
}