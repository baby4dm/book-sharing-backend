package com.booksharing.dto.req;

import com.booksharing.enums.FeedbackStatus;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackRequest(
        FeedbackStatus status,

        @Size(max = 2000, message = "Нотатка занадто довга")
        String moderatorNote) {
}