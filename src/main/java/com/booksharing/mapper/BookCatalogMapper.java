package com.booksharing.mapper;

import com.booksharing.dto.res.BookCatalogEntryResponse;
import com.booksharing.entity.BookCatalogEntry;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookCatalogMapper {

    BookCatalogEntryResponse toResponse(BookCatalogEntry entry);
}