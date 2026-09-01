package com.booksharing.model.dispute;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DisputeMapper {

    @Mapping(target = "exchangeId", source = "exchange.id")
    @Mapping(target = "filedByUserId", source = "filedBy.id")
    @Mapping(target = "filedByName", source = "filedBy.name")
    @Mapping(target = "moderatorId", source = "moderator.id")
    @Mapping(target = "moderatorName", source = "moderator.name")
    DisputeResponse toResponse(Dispute dispute);
}