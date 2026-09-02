package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.res.CommentResponse;
import com.booksharing.dto.req.CreateCommentRequest;
import com.booksharing.dto.req.CreatePostRequest;
import com.booksharing.dto.res.PostResponse;
import com.booksharing.entity.Comment;
import com.booksharing.entity.Post;
import com.booksharing.mapper.FeedMapper;
import com.booksharing.enums.NotificationType;
import com.booksharing.entity.User;

import java.util.List;
import java.util.UUID;

import com.booksharing.repository.CommentRepository;
import com.booksharing.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationService notificationService;
    private final FeedMapper feedMapper;

    public Page<PostResponse> getFeed(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> feedMapper.toResponse(post, commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId()).size()));
    }

    @Transactional
    public PostResponse createPost(User author, CreatePostRequest request) {
        Post post = Post.builder()
                .author(author)
                .content(request.content())
                .photoUrl(request.photoUrl())
                .build();
        post = postRepository.save(post);
        return feedMapper.toResponse(post, 0);
    }

    @Transactional
    public void deletePost(UUID postId, UUID currentUserId, boolean isModerator) {
        Post post = findPostOrThrow(postId);
        if (!post.getAuthor().getId().equals(currentUserId) && !isModerator) {
            throw new AccessDeniedException("Ви можете видаляти лише власні пости");
        }
        postRepository.delete(post);
    }

    public List<CommentResponse> getComments(UUID postId) {
        // 404, якщо поста взагалі не існує, а не тихо порожній список
        findPostOrThrow(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(feedMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(UUID postId, User author, CreateCommentRequest request) {
        Post post = findPostOrThrow(postId);

        Comment comment = Comment.builder()
                .post(post)
                .author(author)
                .content(request.content())
                .build();
        comment = commentRepository.save(comment);

        if (!post.getAuthor().getId().equals(author.getId())) {
            notificationService.notify(
                    post.getAuthor(),
                    NotificationType.NEW_COMMENT,
                    post.getId(),
                    author.getName() + " прокоментував(-ла) ваш пост");
        }

        return feedMapper.toResponse(comment);
    }

    private Post findPostOrThrow(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Пост не знайдено: " + postId));
    }
}