package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "Текст поста не може бути порожнім")
        @Size(max = 4000)
        String content,

        @Size(max = 512)
        String photoUrl) {
}