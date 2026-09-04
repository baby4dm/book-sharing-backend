package com.booksharing.dto.req;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "Email обов'язковий")
        @Email(message = "Некоректний формат email")
        String email,

        @NotBlank(message = "Пароль обов'язковий")
        @Size(min = 8, max = 100, message = "Пароль має бути щонайменше 8 символів")
        String password,

        @NotBlank(message = "Ім'я обов'язкове")
        @Size(max = 255)
        String name) {
}