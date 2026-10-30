package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.ChangeRoleRequest;
import com.booksharing.dto.req.LiftRestrictionRequest;
import com.booksharing.dto.req.ModerationReasonRequest;
import com.booksharing.dto.req.RestrictUserRequest;
import com.booksharing.dto.res.ListingResponse;
import com.booksharing.dto.res.ModerationActionResponse;
import com.booksharing.dto.res.ModerationDisputeResponse;
import com.booksharing.dto.res.ModerationSummaryResponse;
import com.booksharing.dto.res.ModerationUserDetailResponse;
import com.booksharing.dto.res.ModerationUserResponse;
import com.booksharing.entity.Comment;
import com.booksharing.entity.Dispute;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Listing;
import com.booksharing.entity.ModerationAction;
import com.booksharing.entity.Post;
import com.booksharing.entity.Restriction;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.DisputeStatus;
import com.booksharing.enums.ExchangeStatus;
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
import com.booksharing.support.TestData;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Модерація")
class ModerationServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private FeedbackRepository feedbackRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestrictionRepository restrictionRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private ListingPhotoRepository listingPhotoRepository;
    @Mock private PostRepository postRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private ModerationActionRepository moderationActionRepository;
    @Mock private StaffRoleService staffRoleService;
    @Mock private ModerationLogService moderationLog;
    @Mock private NotificationService notificationService;
    @Mock private UserStatsService userStatsService;
    @Spy private ListingMapper listingMapper = Mappers.getMapper(ListingMapper.class);

    @InjectMocks
    private ModerationService moderationService;

    private User moderator;
    private User admin;
    private User user;

    @BeforeEach
    void setUp() {
        moderator = TestData.user("Модератор", UserRole.MODERATOR);
        admin = TestData.user("Адмін", UserRole.ADMIN);
        user = TestData.user("Користувач");
        for (User u : List.of(moderator, admin, user)) {
            when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(userStatsService.statsFor(any())).thenReturn(new UserStatsService.UserExchangeStats(3, 2, 1, 1, 0));
        when(staffRoleService.moderatorBlockers(any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("зведення для панелі модерації")
    void summary() {
        when(disputeRepository.countByStatus(DisputeStatus.OPEN)).thenReturn(2L);
        when(feedbackRepository.countByStatus(FeedbackStatus.NEW)).thenReturn(3L);
        when(userRepository.countByStatusIn(any())).thenReturn(4L);
        when(listingRepository.countByHiddenAtIsNotNull()).thenReturn(5L);

        ModerationSummaryResponse summary = moderationService.summary();

        assertThat(summary.openDisputes()).isEqualTo(2);
        assertThat(summary.newFeedback()).isEqualTo(3);
        assertThat(summary.restrictedUsers()).isEqualTo(4);
        assertThat(summary.hiddenListings()).isEqualTo(5);
    }

    @Test
    @DisplayName("відкриті й розглянуті скарги з даними учасників")
    void disputes() {
        User owner = TestData.user("Власник");
        User reader = TestData.user("Читач");
        Exchange exchange = TestData.exchange(owner, reader, DeliveryMethod.MAIL, ExchangeStatus.DISPUTED);
        Dispute open = Dispute.builder().id(UUID.randomUUID()).exchange(exchange).filedBy(owner)
                .status(DisputeStatus.OPEN).description("Пошкоджено").build();
        Dispute closed = Dispute.builder().id(UUID.randomUUID()).exchange(exchange).filedBy(reader)
                .status(DisputeStatus.DISMISSED).moderator(moderator).build();
        when(disputeRepository.findByStatusOrderByCreatedAtAsc(DisputeStatus.OPEN)).thenReturn(List.of(open));
        when(disputeRepository.findTop100ByStatusNotOrderByResolvedAtDesc(DisputeStatus.OPEN)).thenReturn(List.of(closed));
        when(disputeRepository.findById(open.getId())).thenReturn(Optional.of(open));

        ModerationDisputeResponse openResponse = moderationService.listDisputes(true).get(0);
        ModerationDisputeResponse closedResponse = moderationService.listDisputes(false).get(0);

        assertThat(openResponse.filedByOwner()).isTrue();
        assertThat(openResponse.moderatorName()).isNull();
        assertThat(openResponse.owner().booksGiven()).isEqualTo(3);
        assertThat(openResponse.deliveryMethod()).isEqualTo(DeliveryMethod.MAIL);
        assertThat(closedResponse.filedByOwner()).isFalse();
        assertThat(closedResponse.moderatorName()).isEqualTo("Модератор");
        assertThat(moderationService.getDispute(open.getId()).id()).isEqualTo(open.getId());
        assertThatThrownBy(() -> moderationService.getDispute(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("пошук користувачів за текстом і статусом")
    void searchUsers() {
        ArgumentCaptor<Specification<User>> spec = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(userRepository.findAll(spec.capture(), pageable.capture())).thenReturn(new PageImpl<>(List.of(user)));

        Page<ModerationUserResponse> page = moderationService.searchUsers("  Корист ", UserStatus.ACTIVE, -1, 999);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).booksDamaged()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        Root<User> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        assertThat(spec.getValue().toPredicate(root, query, cb)).isNotNull();

        moderationService.searchUsers(" ", null, 0, 20);
        assertThat(spec.getValue().toPredicate(root, query, cb)).isNull();
    }

    @Test
    @DisplayName("картка користувача з обмеженнями й журналом дій")
    void userDetail() {
        Restriction active = Restriction.builder().id(UUID.randomUUID()).type(RestrictionType.TEMPORARY)
                .endsAt(LocalDateTime.now().plusDays(3)).createdBy(moderator).build();
        Restriction lifted = Restriction.builder().id(UUID.randomUUID()).type(RestrictionType.PERMANENT)
                .liftedAt(LocalDateTime.now().minusDays(1)).liftedBy(admin)
                .dispute(Dispute.builder().id(UUID.randomUUID()).build()).build();
        Restriction expired = Restriction.builder().id(UUID.randomUUID()).type(RestrictionType.TEMPORARY)
                .endsAt(LocalDateTime.now().minusDays(1)).build();
        when(restrictionRepository.findByUserIdOrderByStartsAtDesc(user.getId())).thenReturn(List.of(active, lifted, expired));
        when(moderationActionRepository.findTop30ByTargetUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(
                ModerationAction.builder().id(UUID.randomUUID()).action(ModerationActionType.RESTRICT_USER)
                        .moderator(moderator).targetUser(user).build(),
                ModerationAction.builder().id(UUID.randomUUID()).action(ModerationActionType.HIDE_LISTING).build()));
        when(disputeRepository.countByFiledById(user.getId())).thenReturn(1L);
        when(disputeRepository.countAgainstUser(user.getId())).thenReturn(2L);

        ModerationUserDetailResponse detail = moderationService.getUser(user.getId());

        assertThat(detail.user().name()).isEqualTo("Користувач");
        assertThat(detail.restrictions()).extracting(r -> r.active()).containsExactly(true, false, false);
        assertThat(detail.restrictions().get(0).createdByName()).isEqualTo("Модератор");
        assertThat(detail.restrictions().get(1).liftedByName()).isEqualTo("Адмін");
        assertThat(detail.restrictions().get(1).disputeId()).isNotNull();
        assertThat(detail.actions().get(0).moderatorName()).isEqualTo("Модератор");
        assertThat(detail.actions().get(1).targetUserId()).isNull();
        assertThat(detail.disputesFiled()).isEqualTo(1);
        assertThat(detail.disputesAgainst()).isEqualTo(2);
        assertThatThrownBy(() -> moderationService.getUser(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("тимчасове обмеження закриває попереднє й сповіщає користувача")
    void restrictTemporarily() {
        Restriction previous = Restriction.builder().id(UUID.randomUUID()).endsAt(LocalDateTime.now().plusDays(1)).build();
        Restriction old = Restriction.builder().id(UUID.randomUUID()).endsAt(LocalDateTime.now().minusDays(1)).build();
        when(restrictionRepository.findByUserId(user.getId())).thenReturn(List.of(previous, old));

        moderationService.restrictUser(moderator.getId(), user.getId(),
                new RestrictUserRequest(RestrictionType.TEMPORARY, 7, "  Спам у коментарях "));

        assertThat(user.getStatus()).isEqualTo(UserStatus.RESTRICTED);
        assertThat(user.getRestrictedUntil()).isAfter(LocalDateTime.now().plusDays(6));
        assertThat(previous.getLiftedAt()).isNotNull();
        assertThat(previous.getLiftedBy()).isEqualTo(moderator);
        assertThat(old.getLiftedAt()).isNull();
        verify(notificationService).notify(eq(user), eq(NotificationType.ACCOUNT_RESTRICTED), isNull(),
                org.mockito.ArgumentMatchers.contains("Причина: Спам у коментарях"));
        verify(moderationLog).log(eq(moderator), eq(ModerationActionType.RESTRICT_USER), eq(ModerationTargetType.USER),
                eq(user.getId()), eq(user), eq("Спам у коментарях"), org.mockito.ArgumentMatchers.startsWith("Обмеження на 7 дн."));
    }

    @Test
    @DisplayName("блокування назавжди")
    void block() {
        moderationService.restrictUser(moderator.getId(), user.getId(),
                new RestrictUserRequest(RestrictionType.PERMANENT, null, "Шахрайство"));
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(user.getRestrictedUntil()).isNull();
        verify(moderationLog).log(eq(moderator), eq(ModerationActionType.BLOCK_USER), any(), any(), any(),
                eq("Шахрайство"), eq("Блокування"));
    }

    @Test
    @DisplayName("неприпустимі обмеження")
    void restrictErrors() {
        RestrictUserRequest temp = new RestrictUserRequest(RestrictionType.TEMPORARY, 3, "x");
        assertThatThrownBy(() -> moderationService.restrictUser(moderator.getId(), moderator.getId(), temp))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> moderationService.restrictUser(moderator.getId(), admin.getId(), temp))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> moderationService.restrictUser(moderator.getId(), user.getId(),
                new RestrictUserRequest(RestrictionType.TEMPORARY, null, "x")))
                .isInstanceOf(IllegalArgumentException.class);
        user.setStatus(UserStatus.BLOCKED);
        assertThatThrownBy(() -> moderationService.restrictUser(moderator.getId(), user.getId(), temp))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("вже заблоковано");
        verify(restrictionRepository, never()).save(any());
    }

    @Test
    @DisplayName("зняття обмеження з причиною і без неї")
    void lift() {
        user.setStatus(UserStatus.RESTRICTED);
        moderationService.liftRestriction(moderator.getId(), user.getId(), new LiftRestrictionRequest("  Помилка "));
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(moderationLog).log(eq(moderator), eq(ModerationActionType.LIFT_RESTRICTION), any(), any(), any(),
                eq("Помилка"), isNull());

        user.setStatus(UserStatus.BLOCKED);
        moderationService.liftRestriction(moderator.getId(), user.getId(), null);
        user.setStatus(UserStatus.BLOCKED);
        moderationService.liftRestriction(moderator.getId(), user.getId(), new LiftRestrictionRequest(" "));
        verify(notificationService, org.mockito.Mockito.times(3))
                .notify(eq(user), eq(NotificationType.ACCOUNT_RESTORED), isNull(), anyString());

        assertThatThrownBy(() -> moderationService.liftRestriction(moderator.getId(), user.getId(), null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("адміністратор надає й знімає роль модератора")
    void changeRole() {
        moderationService.changeRole(admin.getId(), user.getId(), new ChangeRoleRequest(UserRole.MODERATOR));
        assertThat(user.getRole()).isEqualTo(UserRole.MODERATOR);
        verify(notificationService).notify(eq(user), eq(NotificationType.MODERATOR_GRANTED), isNull(), anyString());

        moderationService.changeRole(admin.getId(), user.getId(), new ChangeRoleRequest(UserRole.USER));
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        verify(moderationLog).log(eq(admin), eq(ModerationActionType.REVOKE_MODERATOR), any(), any(), any(), isNull(), isNull());
    }

    @Test
    @DisplayName("та сама роль нічого не змінює")
    void sameRole() {
        moderationService.changeRole(admin.getId(), user.getId(), new ChangeRoleRequest(UserRole.USER));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("заборонені зміни ролі")
    void changeRoleErrors() {
        assertThatThrownBy(() -> moderationService.changeRole(admin.getId(), admin.getId(), new ChangeRoleRequest(UserRole.USER)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> moderationService.changeRole(admin.getId(), user.getId(), new ChangeRoleRequest(UserRole.ADMIN)))
                .isInstanceOf(IllegalStateException.class);
        User otherAdmin = TestData.user("Інший адмін", UserRole.ADMIN);
        when(userRepository.findById(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));
        assertThatThrownBy(() -> moderationService.changeRole(admin.getId(), otherAdmin.getId(), new ChangeRoleRequest(UserRole.USER)))
                .isInstanceOf(IllegalStateException.class);
        when(staffRoleService.moderatorBlockers(user)).thenReturn(List.of("2 оголошення"));
        assertThatThrownBy(() -> moderationService.changeRole(admin.getId(), user.getId(), new ChangeRoleRequest(UserRole.MODERATOR)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("2 оголошення");
    }

    @Test
    @DisplayName("прострочені обмеження знімаються автоматично")
    void liftExpired() {
        User restricted = TestData.user("Обмежений");
        restricted.setStatus(UserStatus.RESTRICTED);
        restricted.setRestrictedUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByStatusAndRestrictedUntilBefore(eq(UserStatus.RESTRICTED), any())).thenReturn(List.of(restricted));

        moderationService.liftExpiredRestrictions();

        assertThat(restricted.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(restricted.getRestrictedUntil()).isNull();
        verify(notificationService).notify(eq(restricted), eq(NotificationType.ACCOUNT_RESTORED), isNull(), anyString());
    }

    @Test
    @DisplayName("приховування й відновлення оголошення")
    void hideAndRestore() {
        Listing listing = TestData.listing(user);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> inv.getArgument(0));

        ListingResponse hidden = moderationService.hideListing(moderator.getId(), listing.getId(), new ModerationReasonRequest(" Реклама "));
        assertThat(hidden.hiddenReason()).isEqualTo("Реклама");
        assertThat(listing.getHiddenBy()).isEqualTo(moderator);
        verify(notificationService).notify(eq(user), eq(NotificationType.LISTING_HIDDEN), eq(listing.getId()), anyString());
        assertThatThrownBy(() -> moderationService.hideListing(moderator.getId(), listing.getId(), new ModerationReasonRequest("x")))
                .isInstanceOf(IllegalStateException.class);

        ListingResponse restored = moderationService.restoreListing(moderator.getId(), listing.getId());
        assertThat(restored.hiddenAt()).isNull();
        verify(notificationService).notify(eq(user), eq(NotificationType.LISTING_RESTORED), eq(listing.getId()), anyString());
        assertThatThrownBy(() -> moderationService.restoreListing(moderator.getId(), listing.getId()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> moderationService.restoreListing(moderator.getId(), UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("список прихованих оголошень")
    void hiddenListings() {
        Listing listing = TestData.listing(user);
        listing.setHiddenAt(LocalDateTime.now());
        when(listingRepository.findByHiddenAtIsNotNull(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(listing)));
        assertThat(moderationService.hiddenListings(0, 20).getContent()).hasSize(1);
    }

    @Test
    @DisplayName("видалення поста й коментаря")
    void deleteContent() {
        Post post = Post.builder().id(UUID.randomUUID()).author(user).content("Реклама казино").build();
        Post own = Post.builder().id(UUID.randomUUID()).author(moderator).content("Мій").build();
        Comment comment = Comment.builder().id(UUID.randomUUID()).author(user).content("Образа").build();
        Comment ownComment = Comment.builder().id(UUID.randomUUID()).author(moderator).content("Мій").build();
        when(postRepository.findById(post.getId())).thenReturn(Optional.of(post));
        when(postRepository.findById(own.getId())).thenReturn(Optional.of(own));
        when(commentRepository.findById(comment.getId())).thenReturn(Optional.of(comment));
        when(commentRepository.findById(ownComment.getId())).thenReturn(Optional.of(ownComment));
        ModerationReasonRequest reason = new ModerationReasonRequest("Порушення правил");

        moderationService.deletePost(moderator.getId(), post.getId(), reason);
        moderationService.deletePost(moderator.getId(), own.getId(), reason);
        moderationService.deleteComment(moderator.getId(), comment.getId(), reason);
        moderationService.deleteComment(moderator.getId(), ownComment.getId(), reason);

        verify(postRepository).delete(post);
        verify(commentRepository).delete(comment);
        verify(notificationService, org.mockito.Mockito.times(2))
                .notify(eq(user), eq(NotificationType.CONTENT_REMOVED), isNull(), anyString());
        verify(notificationService, never()).notify(eq(moderator), any(), any(), any());
        verify(moderationLog).log(eq(moderator), eq(ModerationActionType.DELETE_POST), eq(ModerationTargetType.POST),
                eq(post.getId()), eq(user), eq("Порушення правил"), eq("Реклама казино"));
        assertThatThrownBy(() -> moderationService.deletePost(moderator.getId(), UUID.randomUUID(), reason))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> moderationService.deleteComment(moderator.getId(), UUID.randomUUID(), reason))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("журнал дій: усі дії або лише щодо контенту")
    void actions() {
        ModerationAction a = ModerationAction.builder().id(UUID.randomUUID()).action(ModerationActionType.DELETE_POST)
                .targetType(ModerationTargetType.POST).moderator(moderator).build();
        when(moderationActionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(a)));
        when(moderationActionRepository.findByTargetTypeInOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a, a)));

        Page<ModerationActionResponse> all = moderationService.actions(false, 0, 20);
        Page<ModerationActionResponse> content = moderationService.actions(true, -2, 0);

        assertThat(all.getContent()).hasSize(1);
        assertThat(content.getContent()).hasSize(2);
        assertThat(all.getContent().get(0).moderatorName()).isEqualTo("Модератор");
    }
}
