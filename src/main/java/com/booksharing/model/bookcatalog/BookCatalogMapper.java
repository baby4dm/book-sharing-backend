package com.booksharing.model.bookcatalog;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookCatalogMapper {

    BookCatalogEntryResponse toResponse(BookCatalogEntry entry);
}