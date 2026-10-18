package com.booksharing.controller;

import com.booksharing.dto.req.UpdateUserProfileRequest;
import com.booksharing.entity.User;
import com.booksharing.dto.res.UserMeResponse;
import com.booksharing.dto.res.UserPublicProfileResponse;
import com.booksharing.dto.res.UserReviewListResponse;
import com.booksharing.service.ReviewService;
import com.booksharing.service.UserService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ReviewService reviewService;

    @GetMapping("/{id}")
    public UserPublicProfileResponse getPublicProfile(@PathVariable UUID id) {
        return userService.getPublicProfile(id);
    }

    @GetMapping("/{id}/reviews")
    public UserReviewListResponse getReviews(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return reviewService.getForUser(id, page, size);
    }

    @GetMapping("/me")
    public UserMeResponse getMyProfile(@AuthenticationPrincipal User currentUser) {
        return userService.getMyProfile(currentUser);
    }

    @PatchMapping("/me")
    public UserMeResponse updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateMyProfile(currentUser.getId(), request);
    }
}