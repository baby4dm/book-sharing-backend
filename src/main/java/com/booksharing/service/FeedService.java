package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.CreateCommentRequest;
import com.booksharing.dto.req.CreatePostRequest;
import com.booksharing.dto.res.CommentResponse;
import com.booksharing.dto.res.PostLikeResponse;
import com.booksharing.dto.res.PostResponse;
import com.booksharing.entity.Comment;
import com.booksharing.entity.Post;
import com.booksharing.entity.User;
import com.booksharing.enums.NotificationType;
import com.booksharing.mapper.FeedMapper;
import com.booksharing.repository.CommentRepository;
import com.booksharing.repository.PostLikeRepository;
import com.booksharing.repository.PostRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedService {

    private static final int MAX_PAGE_SIZE = 50;

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final PostLikeRepository postLikeRepository;
    private final NotificationService notificationService;
    private final FeedMapper feedMapper;
    private final AccountGuard accountGuard;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    @Transactional(readOnly = true)
    public Page<PostResponse> getFeed(Pageable pageable, User currentUser) {
        Pageable safe = PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), MAX_PAGE_SIZE));
        Page<Post> posts = postRepository.findAllByOrderByCreatedAtDescIdDesc(safe);
        if (posts.isEmpty()) {
            return posts.map(post -> toResponse(post, 0, 0, false));
        }

        List<UUID> ids = posts.getContent().stream().map(Post::getId).toList();
        Map<UUID, Long> comments = toCountMap(commentRepository.countByPostIds(ids));
        Map<UUID, Long> likes = toCountMap(postLikeRepository.countByPostIds(ids));
        Set<UUID> liked = currentUser == null
                ? Set.of()
                : new HashSet<>(postLikeRepository.findLikedPostIds(currentUser.getId(), ids));

        return posts.map(post -> toResponse(
                post,
                comments.getOrDefault(post.getId(), 0L),
                likes.getOrDefault(post.getId(), 0L),
                liked.contains(post.getId())));
    }

    @Transactional
    public PostResponse createPost(User author, CreatePostRequest request) {
        accountGuard.requireCanCreateContent(author.getId());
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Текст поста не може бути порожнім");
        }

        String photoUrl = request.photoUrl() == null || request.photoUrl().isBlank()
                ? null
                : request.photoUrl().trim();
        if (photoUrl != null && !photoUrl.startsWith(backendBaseUrl + "/uploads/")) {
            throw new IllegalArgumentException("Фото має бути завантажене через сервіс");
        }

        Post post = postRepository.save(Post.builder()
                .author(author)
                .content(content)
                .photoUrl(photoUrl)
                .build());
        return toResponse(post, 0, 0, false);
    }

    @Transactional
    public void deletePost(UUID postId, UUID currentUserId, boolean isModerator) {
        Post post = findPostOrThrow(postId);
        if (!post.getAuthor().getId().equals(currentUserId) && !isModerator) {
            throw new AccessDeniedException("Ви можете видаляти лише власні пости");
        }
        postRepository.delete(post);
    }

    @Transactional
    public PostLikeResponse likePost(UUID postId, User currentUser) {
        accountGuard.requireRegularUser(currentUser);
        findPostOrThrow(postId);
        postLikeRepository.insertIfAbsent(postId, currentUser.getId());
        return new PostLikeResponse(postId, postLikeRepository.countByPostId(postId), true);
    }

    /** Зняти вподобання. Ідемпотентно. */
    @Transactional
    public PostLikeResponse unlikePost(UUID postId, User currentUser) {
        findPostOrThrow(postId);
        postLikeRepository.deleteByPostIdAndUserId(postId, currentUser.getId());
        return new PostLikeResponse(postId, postLikeRepository.countByPostId(postId), false);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID postId) {
        findPostOrThrow(postId);
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(feedMapper::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse createComment(UUID postId, User author, CreateCommentRequest request) {
        accountGuard.requireCanCreateContent(author.getId());
        Post post = findPostOrThrow(postId);
        String content = request.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Коментар не може бути порожнім");
        }

        Comment comment = commentRepository.save(Comment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build());

        if (!post.getAuthor().getId().equals(author.getId())) {
            notificationService.notify(
                    post.getAuthor(),
                    NotificationType.NEW_COMMENT,
                    post.getId(),
                    author.getName() + " прокоментував(-ла) ваш пост");
        }

        return feedMapper.toResponse(comment);
    }

    private PostResponse toResponse(Post post, long commentsCount, long likesCount, boolean likedByMe) {
        User author = post.getAuthor();
        return new PostResponse(
                post.getId(),
                author.getId(),
                author.getName(),
                author.getAvatarUrl(),
                post.getContent(),
                post.getPhotoUrl(),
                commentsCount,
                likesCount,
                likedByMe,
                post.getCreatedAt());
    }

    private static Map<UUID, Long> toCountMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> (UUID) row[0],
                row -> (Long) row[1]));
    }

    private Post findPostOrThrow(UUID postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Пост не знайдено: " + postId));
    }
}