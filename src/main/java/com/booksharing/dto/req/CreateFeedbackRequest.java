package com.booksharing.dto.req;

import com.booksharing.enums.FeedbackTopic;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFeedbackRequest(
        @NotNull(message = "Оберіть тему звернення")
        FeedbackTopic topic,

        @NotBlank(message = "Вкажіть email для відповіді")
        @Email(message = "Некоректний email")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Напишіть повідомлення")
        @Size(min = 10, max = 2000, message = "Повідомлення має бути від 10 до 2000 символів")
        String message,

        @Size(max = 512, message = "Посилання занадто довге")
        String link,

        @Size(max = 512)
        String screenshotUrl) {
}