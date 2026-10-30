package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.booksharing.support.TestData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Стрічка спільноти")
class FeedServiceTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private PostLikeRepository postLikeRepository;
    @Mock private NotificationService notificationService;
    @Mock private AccountGuard accountGuard;
    @Spy private FeedMapper feedMapper = Mappers.getMapper(FeedMapper.class);

    @InjectMocks
    private FeedService feedService;

    private User author;
    private User reader;
    private Post post;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(feedService, "backendBaseUrl", BASE_URL);
        author = TestData.user("Автор");
        reader = TestData.user("Читач");
        post = Post.builder().id(UUID.randomUUID()).author(author).content("Що почитати восени?").build();
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static List<Object[]> rows(Object[]... rows) {
        return new ArrayList<>(List.of(rows));
    }

    @Test
    @DisplayName("стрічка показує лічильники коментарів, вподобань і мої вподобання")
    void feed() {
        Post second = Post.builder().id(UUID.randomUUID()).author(reader).content("Другий").build();
        when(postRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post, second)));
        when(commentRepository.countByPostIds(any())).thenReturn(rows(new Object[]{post.getId(), 2L}));
        when(postLikeRepository.countByPostIds(any())).thenReturn(rows(new Object[]{second.getId(), 5L}));
        when(postLikeRepository.findLikedPostIds(eq(reader.getId()), any())).thenReturn(List.of(second.getId()));

        Page<PostResponse> page = feedService.getFeed(PageRequest.of(0, 10), reader);

        PostResponse first = page.getContent().get(0);
        assertThat(first.commentsCount()).isEqualTo(2);
        assertThat(first.likesCount()).isZero();
        assertThat(first.likedByMe()).isFalse();
        PostResponse other = page.getContent().get(1);
        assertThat(other.likesCount()).isEqualTo(5);
        assertThat(other.likedByMe()).isTrue();
    }

    @Test
    @DisplayName("гість бачить стрічку без власних вподобань, розмір сторінки обмежено")
    void feedForGuest() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(postRepository.findAllByOrderByCreatedAtDescIdDesc(pageable.capture()))
                .thenReturn(new PageImpl<>(List.of(post)));

        Page<PostResponse> page = feedService.getFeed(PageRequest.of(0, 500), null);

        assertThat(page.getContent().get(0).likedByMe()).isFalse();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        verify(postLikeRepository, never()).findLikedPostIds(any(), any());
    }

    @Test
    @DisplayName("порожня стрічка не робить додаткових запитів")
    void emptyFeed() {
        when(postRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class))).thenReturn(Page.empty());
        assertThat(feedService.getFeed(PageRequest.of(0, 10), reader).getContent()).isEmpty();
        verify(commentRepository, never()).countByPostIds(any());
    }

    @Test
    @DisplayName("створення поста з фото, завантаженим через сервіс")
    void createPost() {
        PostResponse response = feedService.createPost(author,
                new CreatePostRequest("  Новий пост  ", " " + BASE_URL + "/uploads/p.jpg "));
        assertThat(response.content()).isEqualTo("Новий пост");
        assertThat(response.photoUrl()).isEqualTo(BASE_URL + "/uploads/p.jpg");
        assertThat(response.authorId()).isEqualTo(author.getId());
        verify(accountGuard).requireCanCreateContent(author.getId());
    }

    @Test
    @DisplayName("пост без фото")
    void createPostWithoutPhoto() {
        assertThat(feedService.createPost(author, new CreatePostRequest("Текст", " ")).photoUrl()).isNull();
        assertThat(feedService.createPost(author, new CreatePostRequest("Текст", null)).photoUrl()).isNull();
    }

    @Test
    @DisplayName("порожній пост і стороннє фото відхиляються")
    void createPostInvalid() {
        assertThatThrownBy(() -> feedService.createPost(author, new CreatePostRequest("   ", null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> feedService.createPost(author, new CreatePostRequest("Текст", "https://evil.com/x.jpg")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("завантажене через сервіс");
    }

    @Test
    @DisplayName("видалити пост може автор або модератор")
    void deletePost() {
        feedService.deletePost(post.getId(), author.getId(), false);
        feedService.deletePost(post.getId(), reader.getId(), true);
        verify(postRepository, org.mockito.Mockito.times(2)).delete(post);
        assertThatThrownBy(() -> feedService.deletePost(post.getId(), reader.getId(), false))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> feedService.deletePost(UUID.randomUUID(), author.getId(), false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("вподобання й зняття вподобання")
    void likes() {
        when(postLikeRepository.countByPostId(post.getId())).thenReturn(4L, 3L);

        PostLikeResponse liked = feedService.likePost(post.getId(), reader);
        PostLikeResponse unliked = feedService.unlikePost(post.getId(), reader);

        assertThat(liked.likedByMe()).isTrue();
        assertThat(liked.likesCount()).isEqualTo(4);
        assertThat(unliked.likedByMe()).isFalse();
        assertThat(unliked.likesCount()).isEqualTo(3);
        verify(accountGuard).requireRegularUser(reader);
        verify(postLikeRepository).insertIfAbsent(post.getId(), reader.getId());
        verify(postLikeRepository).deleteByPostIdAndUserId(post.getId(), reader.getId());
    }

    @Test
    @DisplayName("коментарі поста")
    void comments() {
        Comment c = Comment.builder().id(UUID.randomUUID()).post(post).author(reader).content("Раджу Франка").build();
        when(commentRepository.findByPostIdOrderByCreatedAtAsc(post.getId())).thenReturn(List.of(c));

        List<CommentResponse> comments = feedService.getComments(post.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).authorName()).isEqualTo("Читач");
        assertThat(comments.get(0).postId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("коментар до чужого поста сповіщає автора")
    void commentNotifiesAuthor() {
        CommentResponse response = feedService.createComment(post.getId(), reader, new CreateCommentRequest("  Цікаво  "));
        assertThat(response.content()).isEqualTo("Цікаво");
        verify(notificationService).notify(eq(author), eq(NotificationType.NEW_COMMENT), eq(post.getId()), anyString());
    }

    @Test
    @DisplayName("коментар до власного поста без сповіщення")
    void ownComment() {
        feedService.createComment(post.getId(), author, new CreateCommentRequest("Доповнюю"));
        verify(notificationService, never()).notify(any(), any(), any(), any());
    }

    @Test
    @DisplayName("порожній коментар відхиляється")
    void emptyComment() {
        assertThatThrownBy(() -> feedService.createComment(post.getId(), reader, new CreateCommentRequest(" ")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
