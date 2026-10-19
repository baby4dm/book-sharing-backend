package com.booksharing.dto.res;

import java.util.UUID;

public record PostLikeResponse(UUID postId, long likesCount, boolean likedByMe) {
}