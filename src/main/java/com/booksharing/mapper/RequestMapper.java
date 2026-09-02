package com.booksharing.mapper;

import com.booksharing.entity.Request;
import com.booksharing.dto.res.RequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "listingId", source = "listing.id")
    @Mapping(target = "listingBookTitle", source = "listing.bookCatalogEntry.title")
    @Mapping(target = "requesterId", source = "requester.id")
    @Mapping(target = "requesterName", source = "requester.name")
    @Mapping(target = "requesterAvatarUrl", source = "requester.avatarUrl")
    @Mapping(target = "requesterRatingAvg", source = "requester.ratingAvg")
    RequestResponse toResponse(Request request);
}