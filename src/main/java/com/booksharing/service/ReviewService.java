package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.CreateReviewRequest;
import com.booksharing.dto.res.ExchangeResponse;
import com.booksharing.dto.res.UserReviewListResponse;
import com.booksharing.dto.res.UserReviewResponse;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Review;
import com.booksharing.entity.User;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.NotificationType;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ReviewRepository;
import com.booksharing.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ExchangeService exchangeService;

    @Transactional
    public ExchangeResponse create(
            UUID exchangeId, UUID currentUserId, CreateReviewRequest request) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ResourceNotFoundException("Обмін не знайдено: " + exchangeId));

        boolean isOwner = exchange.getOwner().getId().equals(currentUserId);
        boolean isReader = exchange.getReader().getId().equals(currentUserId);
        if (!isOwner && !isReader) {
            throw new AccessDeniedException("Ви не берете участі в цьому обміні");
        }
        if (exchange.getStatus() != ExchangeStatus.COMPLETED) {
            throw new IllegalStateException(
                    "Відгук можна залишити лише після завершення обміну");
        }
        if (reviewRepository.existsByExchangeIdAndAuthorId(exchangeId, currentUserId)) {
            throw new IllegalStateException("Ви вже залишили відгук за цей обмін");
        }

        User author = isOwner ? exchange.getOwner() : exchange.getReader();
        User target = isOwner ? exchange.getReader() : exchange.getOwner();

        String comment = request.comment() == null || request.comment().isBlank()
                ? null
                : request.comment().trim();

        reviewRepository.save(Review.builder()
                .exchange(exchange)
                .author(author)
                .target(target)
                .rating(request.rating().shortValue())
                .comment(comment)
                .build());

        Double average = reviewRepository.averageRatingByTargetId(target.getId());
        if (average != null) {
            target.setRatingAvg(
                    BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP));
            userRepository.save(target);
        }

        notificationService.notify(
                target,
                NotificationType.REVIEW_RECEIVED,
                exchange.getId(),
                "Ви отримали новий відгук від " + author.getName() + ".");

        return exchangeService.getById(exchangeId);
    }

    /** Відгуки про користувача (для профілю), нові спочатку. */
    @Transactional(readOnly = true)
    public UserReviewListResponse getForUser(UUID userId, int page, int size) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Користувача не знайдено: " + userId);
        }

        Page<Review> result = reviewRepository.findByTargetIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));

        return new UserReviewListResponse(
                result.getContent().stream().map(this::toUserReviewResponse).toList(),
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements());
    }

    private UserReviewResponse toUserReviewResponse(Review review) {
        Exchange exchange = review.getExchange();
        User author = review.getAuthor();
        boolean authorIsOwner = exchange.getOwner().getId().equals(author.getId());

        return new UserReviewResponse(
                review.getId(),
                exchange.getId(),
                exchange.getListing().getBookCatalogEntry().getTitle(),
                author.getId(),
                author.getName(),
                author.getAvatarUrl(),
                authorIsOwner ? "OWNER" : "READER",
                review.getRating(),
                review.getComment(),
                review.getCreatedAt());
    }
}