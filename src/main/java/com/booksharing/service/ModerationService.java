package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.LiftRestrictionRequest;
import com.booksharing.dto.req.ModerationReasonRequest;
import com.booksharing.dto.req.RestrictUserRequest;
import com.booksharing.dto.res.ExchangeParticipantResponse;
import com.booksharing.dto.res.ListingResponse;
import com.booksharing.dto.res.ModerationActionResponse;
import com.booksharing.dto.res.ModerationDisputeResponse;
import com.booksharing.dto.res.ModerationSummaryResponse;
import com.booksharing.dto.res.ModerationUserDetailResponse;
import com.booksharing.dto.res.ModerationUserResponse;
import com.booksharing.dto.res.RestrictionResponse;
import com.booksharing.entity.Comment;
import com.booksharing.entity.Dispute;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Listing;
import com.booksharing.entity.ListingPhoto;
import com.booksharing.entity.ModerationAction;
import com.booksharing.entity.Post;
import com.booksharing.entity.Restriction;
import com.booksharing.entity.User;
import com.booksharing.enums.DisputeStatus;
import com.booksharing.enums.FeedbackStatus;
import com.booksharing.enums.ModerationActionType;
import com.booksharing.enums.ModerationTargetType;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.RestrictionType;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.mapper.ListingMapper;
import com.booksharing.repository.CommentRepository;
import com.booksharing.repository.DisputeRepository;
import com.booksharing.repository.FeedbackRepository;
import com.booksharing.repository.ListingPhotoRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.ModerationActionRepository;
import com.booksharing.repository.PostRepository;
import com.booksharing.repository.RestrictionRepository;
import com.booksharing.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final DisputeRepository disputeRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final RestrictionRepository restrictionRepository;
    private final ListingRepository listingRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final ModerationLogService moderationLog;
    private final NotificationService notificationService;
    private final UserStatsService userStatsService;
    private final ListingMapper listingMapper;


    @Transactional(readOnly = true)
    public ModerationSummaryResponse summary() {
        return new ModerationSummaryResponse(
                disputeRepository.countByStatus(DisputeStatus.OPEN),
                feedbackRepository.countByStatus(FeedbackStatus.NEW),
                userRepository.countByStatusIn(List.of(UserStatus.RESTRICTED, UserStatus.BLOCKED)),
                listingRepository.countByHiddenAtIsNotNull());
    }

    @Transactional(readOnly = true)
    public List<ModerationDisputeResponse> listDisputes(boolean open) {
        List<Dispute> disputes = open
                ? disputeRepository.findByStatusOrderByCreatedAtAsc(DisputeStatus.OPEN)
                : disputeRepository.findTop100ByStatusNotOrderByResolvedAtDesc(DisputeStatus.OPEN);
        return disputes.stream().map(this::toDisputeResponse).toList();
    }

    @Transactional(readOnly = true)
    public ModerationDisputeResponse getDispute(UUID id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Скаргу не знайдено: " + id));
        return toDisputeResponse(dispute);
    }


    @Transactional(readOnly = true)
    public Page<ModerationUserResponse> searchUsers(String query, UserStatus status, int page, int size) {
        Specification<User> spec = (root, q, cb) -> null;
        if (query != null && !query.isBlank()) {
            String pattern = "%" + query.strip().toLowerCase() + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern)));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return userRepository.findAll(spec, pageable).map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public ModerationUserDetailResponse getUser(UUID userId) {
        User user = findUser(userId);
        LocalDateTime now = LocalDateTime.now();
        List<RestrictionResponse> restrictions = restrictionRepository.findByUserIdOrderByStartsAtDesc(userId)
                .stream()
                .map(r -> toRestrictionResponse(r, now))
                .toList();
        List<ModerationActionResponse> actions = moderationActionRepository
                .findTop30ByTargetUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ModerationService::toActionResponse)
                .toList();
        return new ModerationUserDetailResponse(
                toUserResponse(user),
                restrictions,
                actions,
                disputeRepository.countByFiledById(userId),
                disputeRepository.countAgainstUser(userId));
    }

    @Transactional
    public ModerationUserDetailResponse restrictUser(UUID moderatorId, UUID userId, RestrictUserRequest request) {
        User moderator = findUser(moderatorId);
        User user = findUser(userId);

        if (user.getId().equals(moderatorId)) {
            throw new IllegalArgumentException("Не можна обмежити власний обліковий запис");
        }
        if (user.getRole() != UserRole.USER) {
            throw new IllegalStateException("Обмежувати можна лише звичайних користувачів, не модераторів");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new IllegalStateException("Користувача вже заблоковано. Спершу зніміть блокування");
        }

        boolean permanent = request.type() == RestrictionType.PERMANENT;
        if (!permanent && request.days() == null) {
            throw new IllegalArgumentException("Вкажіть, на скільки днів обмежити");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endsAt = permanent ? null : now.plusDays(request.days());

        closeActiveRestrictions(user, moderator, now);

        restrictionRepository.save(Restriction.builder()
                .user(user)
                .type(request.type())
                .reason(request.reason().strip())
                .endsAt(endsAt)
                .createdBy(moderator)
                .build());

        user.setStatus(permanent ? UserStatus.BLOCKED : UserStatus.RESTRICTED);
        user.setRestrictedUntil(endsAt);
        userRepository.save(user);

        notificationService.notify(
                user,
                NotificationType.ACCOUNT_RESTRICTED,
                null,
                permanent
                        ? "Ваш обліковий запис заблоковано модератором. Причина: " + request.reason().strip()
                        : "Ваш обліковий запис обмежено до " + endsAt.format(DATE)
                                + ". Причина: " + request.reason().strip());

        moderationLog.log(moderator,
                permanent ? ModerationActionType.BLOCK_USER : ModerationActionType.RESTRICT_USER,
                ModerationTargetType.USER, user.getId(), user, request.reason().strip(),
                permanent ? "Блокування" : "Обмеження на " + request.days() + " дн. (до " + endsAt.format(DATE) + ")");

        return getUser(userId);
    }

    @Transactional
    public ModerationUserDetailResponse liftRestriction(UUID moderatorId, UUID userId, LiftRestrictionRequest request) {
        User moderator = findUser(moderatorId);
        User user = findUser(userId);
        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalStateException("У користувача немає чинних обмежень");
        }

        closeActiveRestrictions(user, moderator, LocalDateTime.now());
        user.setStatus(UserStatus.ACTIVE);
        user.setRestrictedUntil(null);
        userRepository.save(user);

        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? null
                : request.reason().strip();
        notificationService.notify(
                user,
                NotificationType.ACCOUNT_RESTORED,
                null,
                "Модератор зняв обмеження з вашого облікового запису");
        moderationLog.log(moderator, ModerationActionType.LIFT_RESTRICTION,
                ModerationTargetType.USER, user.getId(), user, reason, null);

        return getUser(userId);
    }

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void liftExpiredRestrictions() {
        LocalDateTime now = LocalDateTime.now();
        for (User user : userRepository.findByStatusAndRestrictedUntilBefore(UserStatus.RESTRICTED, now)) {
            user.setStatus(UserStatus.ACTIVE);
            user.setRestrictedUntil(null);
            userRepository.save(user);
            notificationService.notify(
                    user,
                    NotificationType.ACCOUNT_RESTORED,
                    null,
                    "Строк обмеження минув — ви знову можете додавати книги й подавати заявки");
        }
    }


    @Transactional
    public ListingResponse hideListing(UUID moderatorId, UUID listingId, ModerationReasonRequest request) {
        User moderator = findUser(moderatorId);
        Listing listing = findListing(listingId);
        if (listing.isHidden()) {
            throw new IllegalStateException("Оголошення вже приховано");
        }

        String reason = request.reason().strip();
        listing.setHiddenAt(LocalDateTime.now());
        listing.setHiddenReason(reason);
        listing.setHiddenBy(moderator);
        listing = listingRepository.save(listing);

        String title = listing.getBookCatalogEntry().getTitle();
        notificationService.notify(
                listing.getOwner(),
                NotificationType.LISTING_HIDDEN,
                listing.getId(),
                "Модератор приховав ваше оголошення «" + title + "». Причина: " + reason);
        moderationLog.log(moderator, ModerationActionType.HIDE_LISTING, ModerationTargetType.LISTING,
                listing.getId(), listing.getOwner(), reason, title);

        return listingMapper.toResponse(listing, photoUrlsOf(listing.getId()));
    }

    @Transactional
    public ListingResponse restoreListing(UUID moderatorId, UUID listingId) {
        User moderator = findUser(moderatorId);
        Listing listing = findListing(listingId);
        if (!listing.isHidden()) {
            throw new IllegalStateException("Оголошення не приховане");
        }

        listing.setHiddenAt(null);
        listing.setHiddenReason(null);
        listing.setHiddenBy(null);
        listing = listingRepository.save(listing);

        String title = listing.getBookCatalogEntry().getTitle();
        notificationService.notify(
                listing.getOwner(),
                NotificationType.LISTING_RESTORED,
                listing.getId(),
                "Ваше оголошення «" + title + "» знову в каталозі");
        moderationLog.log(moderator, ModerationActionType.RESTORE_LISTING, ModerationTargetType.LISTING,
                listing.getId(), listing.getOwner(), null, title);

        return listingMapper.toResponse(listing, photoUrlsOf(listing.getId()));
    }

    @Transactional(readOnly = true)
    public Page<ListingResponse> hiddenListings(int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "hiddenAt"));
        return listingRepository.findByHiddenAtIsNotNull(pageable)
                .map(l -> listingMapper.toResponse(l, photoUrlsOf(l.getId())));
    }


    @Transactional
    public void deletePost(UUID moderatorId, UUID postId, ModerationReasonRequest request) {
        User moderator = findUser(moderatorId);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Пост не знайдено: " + postId));
        User author = post.getAuthor();
        String content = post.getContent();
        String reason = request.reason().strip();

        postRepository.delete(post);

        if (!author.getId().equals(moderatorId)) {
            notificationService.notify(
                    author,
                    NotificationType.CONTENT_REMOVED,
                    null,
                    "Модератор видалив ваш пост у спільноті. Причина: " + reason);
        }
        moderationLog.log(moderator, ModerationActionType.DELETE_POST, ModerationTargetType.POST,
                postId, author, reason, content);
    }

    @Transactional
    public void deleteComment(UUID moderatorId, UUID commentId, ModerationReasonRequest request) {
        User moderator = findUser(moderatorId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Коментар не знайдено: " + commentId));
        User author = comment.getAuthor();
        String content = comment.getContent();
        String reason = request.reason().strip();

        commentRepository.delete(comment);

        if (!author.getId().equals(moderatorId)) {
            notificationService.notify(
                    author,
                    NotificationType.CONTENT_REMOVED,
                    null,
                    "Модератор видалив ваш коментар. Причина: " + reason);
        }
        moderationLog.log(moderator, ModerationActionType.DELETE_COMMENT, ModerationTargetType.COMMENT,
                commentId, author, reason, content);
    }


    @Transactional(readOnly = true)
    public Page<ModerationActionResponse> actions(boolean contentOnly, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        Page<ModerationAction> actions = contentOnly
                ? moderationActionRepository.findByTargetTypeInOrderByCreatedAtDesc(
                        List.of(ModerationTargetType.LISTING, ModerationTargetType.POST, ModerationTargetType.COMMENT),
                        pageable)
                : moderationActionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return actions.map(ModerationService::toActionResponse);
    }


    private void closeActiveRestrictions(User user, User moderator, LocalDateTime now) {
        for (Restriction restriction : restrictionRepository.findByUserId(user.getId())) {
            if (isActive(restriction, now)) {
                restriction.setLiftedAt(now);
                restriction.setLiftedBy(moderator);
                restrictionRepository.save(restriction);
            }
        }
    }

    private static boolean isActive(Restriction restriction, LocalDateTime now) {
        return restriction.getLiftedAt() == null
                && (restriction.getEndsAt() == null || restriction.getEndsAt().isAfter(now));
    }

    private ModerationDisputeResponse toDisputeResponse(Dispute dispute) {
        Exchange exchange = dispute.getExchange();
        User owner = exchange.getOwner();
        User reader = exchange.getReader();
        return new ModerationDisputeResponse(
                dispute.getId(),
                exchange.getId(),
                dispute.getStatus(),
                dispute.getDescription(),
                dispute.getCreatedAt(),
                dispute.getResolvedAt(),
                dispute.getResolutionComment(),
                dispute.getModerator() == null ? null : dispute.getModerator().getName(),
                dispute.getFiledBy().getId(),
                dispute.getFiledBy().getName(),
                dispute.getFiledBy().getId().equals(owner.getId()),
                exchange.getListing().getBookCatalogEntry().getTitle(),
                exchange.getListing().getBookCatalogEntry().getAuthor(),
                exchange.getListing().getBookCatalogEntry().getCoverUrl(),
                exchange.getStatus(),
                exchange.getDeliveryMethod(),
                toParticipant(owner),
                toParticipant(reader));
    }

    private ExchangeParticipantResponse toParticipant(User user) {
        UserStatsService.UserExchangeStats stats = userStatsService.statsFor(user);
        return new ExchangeParticipantResponse(
                user.getId(),
                user.getName(),
                user.getAvatarUrl(),
                user.getRatingAvg(),
                stats.booksGiven(),
                stats.booksTaken(),
                stats.booksOverdue());
    }

    private ModerationUserResponse toUserResponse(User user) {
        UserStatsService.UserExchangeStats stats = userStatsService.statsFor(user);
        return new ModerationUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getRestrictedUntil(),
                user.getRatingAvg(),
                stats.booksGiven(),
                stats.booksTaken(),
                stats.booksOverdue(),
                stats.booksDamaged(),
                user.getCreatedAt());
    }

    private static RestrictionResponse toRestrictionResponse(Restriction r, LocalDateTime now) {
        return new RestrictionResponse(
                r.getId(),
                r.getType(),
                r.getReason(),
                r.getStartsAt(),
                r.getEndsAt(),
                r.getLiftedAt(),
                r.getCreatedBy() == null ? null : r.getCreatedBy().getName(),
                r.getLiftedBy() == null ? null : r.getLiftedBy().getName(),
                r.getDispute() == null ? null : r.getDispute().getId(),
                isActive(r, now));
    }

    private static ModerationActionResponse toActionResponse(ModerationAction a) {
        return new ModerationActionResponse(
                a.getId(),
                a.getAction(),
                a.getTargetType(),
                a.getTargetId(),
                a.getTargetUser() == null ? null : a.getTargetUser().getId(),
                a.getTargetUser() == null ? null : a.getTargetUser().getName(),
                a.getModerator() == null ? null : a.getModerator().getId(),
                a.getModerator() == null ? null : a.getModerator().getName(),
                a.getReason(),
                a.getDetails(),
                a.getCreatedAt());
    }

    private List<String> photoUrlsOf(UUID listingId) {
        return listingPhotoRepository.findByListingId(listingId).stream()
                .map(ListingPhoto::getUrl)
                .toList();
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + id));
    }

    private Listing findListing(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Оголошення не знайдено: " + id));
    }
}
