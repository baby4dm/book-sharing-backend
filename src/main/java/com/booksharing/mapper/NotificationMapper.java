package com.booksharing.mapper;

import com.booksharing.entity.Notification;
import com.booksharing.dto.res.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}