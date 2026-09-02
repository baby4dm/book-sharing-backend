package com.booksharing.dto.req;

import com.booksharing.mapper.UserMapper;
import jakarta.validation.constraints.Size;

/**
 * Тіло PATCH /api/users/me. Усі поля опційні (часткове оновлення) —
 * {@code null} означає "не чіпати це поле", а не "очистити його".
 * У {@link UserMapper#updateEntityFromRequest} це реалізовано через
 * {@code NullValuePropertyMappingStrategy.IGNORE}.
 */
public record UpdateUserProfileRequest(

        @Size(min = 1, max = 255, message = "Ім'я має бути від 1 до 255 символів")
        String name,

        @Size(max = 512, message = "URL аватара занадто довгий")
        String avatarUrl,

        @Size(max = 255, message = "Назва міста занадто довга")
        String city,

        @Size(max = 2000, message = "Опис про себе занадто довгий")
        String bio) {
}