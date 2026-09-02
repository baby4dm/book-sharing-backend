package com.booksharing.controller;

import com.booksharing.dto.res.PostResponse;
import com.booksharing.dto.res.CommentResponse;
import com.booksharing.dto.req.CreateCommentRequest;
import com.booksharing.dto.req.CreatePostRequest;
import com.booksharing.service.FeedService;
import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final FeedService feedService;

    @GetMapping
    public Page<PostResponse> getFeed(@PageableDefault(size = 20) Pageable pageable) {
        return feedService.getFeed(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreatePostRequest request) {
        return feedService.createPost(currentUser, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        boolean isModerator = currentUser.getRole() == UserRole.MODERATOR || currentUser.getRole() == UserRole.ADMIN;
        feedService.deletePost(id, currentUser.getId(), isModerator);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> getComments(@PathVariable UUID id) {
        return feedService.getComments(id);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse createComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateCommentRequest request) {
        return feedService.createComment(id, currentUser, request);
    }
}