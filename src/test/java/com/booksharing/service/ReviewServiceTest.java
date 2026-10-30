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
import com.booksharing.dto.req.CreateReviewRequest;
import com.booksharing.dto.res.UserReviewListResponse;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Review;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.NotificationType;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ReviewRepository;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Відгуки після обміну")
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ExchangeRepository exchangeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExchangeService exchangeService;

    @InjectMocks
    private ReviewService reviewService;

    private User owner;
    private User reader;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        owner = TestData.user("Власник");
        reader = TestData.user("Читач");
        exchange = TestData.exchange(owner, reader, DeliveryMethod.PICKUP, ExchangeStatus.COMPLETED);
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
    }

    @Test
    @DisplayName("читач оцінює власника, рейтинг перераховується")
    void readerReviewsOwner() {
        when(reviewRepository.averageRatingByTargetId(owner.getId())).thenReturn(4.666);

        reviewService.create(exchange.getId(), reader.getId(), new CreateReviewRequest(5, "  Чудова книга  "));

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review review = captor.getValue();
        assertThat(review.getAuthor()).isEqualTo(reader);
        assertThat(review.getTarget()).isEqualTo(owner);
        assertThat(review.getRating()).isEqualTo((short) 5);
        assertThat(review.getComment()).isEqualTo("Чудова книга");
        assertThat(owner.getRatingAvg()).isEqualByComparingTo(new BigDecimal("4.67"));
        verify(userRepository).save(owner);
        verify(notificationService).notify(eq(owner), eq(NotificationType.REVIEW_RECEIVED), eq(exchange.getId()), anyString());
        verify(exchangeService).getById(exchange.getId());
    }

    @Test
    @DisplayName("власник оцінює читача без коментаря")
    void ownerReviewsReader() {
        when(reviewRepository.averageRatingByTargetId(reader.getId())).thenReturn(null);
        reviewService.create(exchange.getId(), owner.getId(), new CreateReviewRequest(4, " "));
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getTarget()).isEqualTo(reader);
        assertThat(captor.getValue().getComment()).isNull();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("відгук можна залишити лише після завершення обміну")
    void notCompleted() {
        exchange.setStatus(ExchangeStatus.RETURN_PENDING);
        assertThatThrownBy(() -> reviewService.create(exchange.getId(), reader.getId(), new CreateReviewRequest(5, null)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("другий відгук на той самий обмін не приймається")
    void onlyOnce() {
        when(reviewRepository.existsByExchangeIdAndAuthorId(exchange.getId(), reader.getId())).thenReturn(true);
        assertThatThrownBy(() -> reviewService.create(exchange.getId(), reader.getId(), new CreateReviewRequest(5, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("вже залишили");
    }

    @Test
    @DisplayName("сторонній користувач не залишає відгук")
    void stranger() {
        assertThatThrownBy(() -> reviewService.create(exchange.getId(), UUID.randomUUID(), new CreateReviewRequest(5, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("неіснуючий обмін")
    void exchangeNotFound() {
        assertThatThrownBy(() -> reviewService.create(UUID.randomUUID(), reader.getId(), new CreateReviewRequest(5, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("відгуки про користувача з роллю автора")
    void reviewsForUser() {
        Review fromReader = Review.builder().id(UUID.randomUUID()).exchange(exchange).author(reader).target(owner)
                .rating((short) 5).comment("Дякую").build();
        Review fromOwner = Review.builder().id(UUID.randomUUID()).exchange(exchange).author(owner).target(reader)
                .rating((short) 4).build();
        when(userRepository.existsById(owner.getId())).thenReturn(true);
        when(reviewRepository.findByTargetIdOrderByCreatedAtDesc(eq(owner.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fromReader, fromOwner)));

        UserReviewListResponse result = reviewService.getForUser(owner.getId(), -1, 100);

        assertThat(result.items()).hasSize(2);
        assertThat(result.items().get(0).authorRole()).isEqualTo("READER");
        assertThat(result.items().get(0).bookTitle()).isEqualTo("Лісова пісня");
        assertThat(result.items().get(1).authorRole()).isEqualTo("OWNER");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("відгуки неіснуючого користувача")
    void reviewsForMissingUser() {
        assertThatThrownBy(() -> reviewService.getForUser(UUID.randomUUID(), 0, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}