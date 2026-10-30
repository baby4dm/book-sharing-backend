package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.CreateDisputeRequest;
import com.booksharing.dto.req.ResolveDisputeRequest;
import com.booksharing.dto.res.DisputeResponse;
import com.booksharing.entity.Dispute;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Restriction;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.DisputeStatus;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ModerationActionType;
import com.booksharing.enums.ModerationTargetType;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.RestrictionType;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.mapper.DisputeMapper;
import com.booksharing.repository.DisputeRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.RestrictionRepository;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
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
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Скарги між учасниками обміну")
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private RestrictionRepository restrictionRepository;
    @Mock private ExchangeRepository exchangeRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExchangeService exchangeService;
    @Mock private ModerationLogService moderationLog;
    @Spy private DisputeMapper disputeMapper = Mappers.getMapper(DisputeMapper.class);

    @InjectMocks
    private DisputeService disputeService;

    private User owner;
    private User reader;
    private User moderator;
    private Exchange exchange;

    @BeforeEach
    void setUp() {
        owner = TestData.user("Власник");
        reader = TestData.user("Читач");
        moderator = TestData.user("Модератор", UserRole.MODERATOR);
        exchange = TestData.exchange(owner, reader, DeliveryMethod.PICKUP, ExchangeStatus.IN_READING);
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(userRepository.findById(moderator.getId())).thenReturn(Optional.of(moderator));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(userRepository.findById(reader.getId())).thenReturn(Optional.of(reader));
        when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
            Dispute d = inv.getArgument(0);
            if (d.getId() == null) {
                d.setId(UUID.randomUUID());
            }
            return d;
        });
    }

    private Dispute openDispute(User filedBy) {
        Dispute dispute = Dispute.builder()
                .id(UUID.randomUUID())
                .exchange(exchange)
                .filedBy(filedBy)
                .description("Книгу не повернуто")
                .status(DisputeStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .build();
        when(disputeRepository.findById(dispute.getId())).thenReturn(Optional.of(dispute));
        return dispute;
    }

    @Test
    @DisplayName("читач подає скаргу, обмін переходить у спір")
    void fileDispute() {
        DisputeResponse response = disputeService.fileDispute(
                exchange.getId(), reader.getId(), new CreateDisputeRequest("Власник не відповідає"));

        assertThat(response.status()).isEqualTo(DisputeStatus.OPEN);
        assertThat(response.filedByUserId()).isEqualTo(reader.getId());
        assertThat(response.exchangeId()).isEqualTo(exchange.getId());
        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.DISPUTED);
        assertThat(exchange.getDisputeReason()).isEqualTo("Власник не відповідає");
        verify(notificationService).notify(eq(owner), eq(NotificationType.DISPUTE_FILED), any(), anyString());
    }

    @Test
    @DisplayName("власник подає скаргу на читача під час повернення")
    void ownerFiles() {
        exchange.setStatus(ExchangeStatus.RETURN_PENDING);
        disputeService.fileDispute(exchange.getId(), owner.getId(), new CreateDisputeRequest("Пошкоджено"));
        verify(notificationService).notify(eq(reader), eq(NotificationType.DISPUTE_FILED), any(), anyString());
    }

    @Test
    @DisplayName("скаргу на прострочений обмін теж приймають")
    void overdueExchange() {
        exchange.setStatus(ExchangeStatus.OVERDUE);
        assertThat(disputeService.fileDispute(exchange.getId(), owner.getId(), new CreateDisputeRequest("Прострочено")))
                .isNotNull();
    }

    @Test
    @DisplayName("до початку читання скаргу подати не можна")
    void wrongStage() {
        exchange.setStatus(ExchangeStatus.HANDOVER_PENDING);
        assertThatThrownBy(() -> disputeService.fileDispute(exchange.getId(), reader.getId(), new CreateDisputeRequest("x")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("другу відкриту скаргу не приймають")
    void alreadyOpen() {
        Dispute existing = openDispute(owner);
        when(disputeRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(existing));
        assertThatThrownBy(() -> disputeService.fileDispute(exchange.getId(), reader.getId(), new CreateDisputeRequest("x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("вже є відкрита");
    }

    @Test
    @DisplayName("сторонній користувач не може подати скаргу")
    void stranger() {
        assertThatThrownBy(() -> disputeService.fileDispute(exchange.getId(), UUID.randomUUID(), new CreateDisputeRequest("x")))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("скарга на неіснуючий обмін")
    void exchangeNotFound() {
        assertThatThrownBy(() -> disputeService.fileDispute(UUID.randomUUID(), reader.getId(), new CreateDisputeRequest("x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("скаргу бачать учасники й модератори, але не сторонні")
    void getById() {
        Dispute dispute = openDispute(reader);
        User stranger = TestData.user("Сторонній");
        when(userRepository.findById(stranger.getId())).thenReturn(Optional.of(stranger));

        assertThat(disputeService.getById(dispute.getId(), owner.getId()).id()).isEqualTo(dispute.getId());
        assertThat(disputeService.getById(dispute.getId(), reader.getId())).isNotNull();
        assertThat(disputeService.getById(dispute.getId(), moderator.getId())).isNotNull();
        assertThatThrownBy(() -> disputeService.getById(dispute.getId(), stranger.getId()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> disputeService.getById(dispute.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("адміністратор теж бачить скаргу")
    void adminSees() {
        Dispute dispute = openDispute(reader);
        User admin = TestData.user("Адмін", UserRole.ADMIN);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));
        assertThat(disputeService.getById(dispute.getId(), admin.getId())).isNotNull();
    }

    @Test
    @DisplayName("неіснуюча скарга")
    void disputeNotFound() {
        assertThatThrownBy(() -> disputeService.getById(UUID.randomUUID(), owner.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("список відкритих скарг")
    void listOpen() {
        Dispute first = openDispute(reader);
        Dispute second = openDispute(owner);
        when(disputeRepository.findByStatus(DisputeStatus.OPEN)).thenReturn(List.of(first, second));
        assertThat(disputeService.listOpenDisputes()).hasSize(2);
    }

    @Test
    @DisplayName("рішення не може бути статусом OPEN")
    void resolveWithOpen() {
        Dispute dispute = openDispute(reader);
        assertThatThrownBy(() -> disputeService.resolve(dispute.getId(), moderator.getId(),
                new ResolveDisputeRequest(DisputeStatus.OPEN, "x")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("розглянуту скаргу не можна розглянути вдруге")
    void resolveTwice() {
        Dispute dispute = openDispute(reader);
        dispute.setStatus(DisputeStatus.DISMISSED);
        assertThatThrownBy(() -> disputeService.resolve(dispute.getId(), moderator.getId(),
                new ResolveDisputeRequest(DisputeStatus.DISMISSED, "x")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("неіснуючий модератор")
    void moderatorNotFound() {
        Dispute dispute = openDispute(reader);
        assertThatThrownBy(() -> disputeService.resolve(dispute.getId(), UUID.randomUUID(),
                new ResolveDisputeRequest(DisputeStatus.DISMISSED, "x")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("відхилена скарга завершує обмін без санкцій")
    void dismiss() {
        exchange.setStatus(ExchangeStatus.DISPUTED);
        Dispute dispute = openDispute(reader);

        DisputeResponse response = disputeService.resolve(dispute.getId(), moderator.getId(),
                new ResolveDisputeRequest(DisputeStatus.DISMISSED, "Недостатньо доказів"));

        assertThat(response.status()).isEqualTo(DisputeStatus.DISMISSED);
        assertThat(response.moderatorName()).isEqualTo("Модератор");
        assertThat(dispute.getResolvedAt()).isNotNull();
        verify(exchangeService).finalizeReturnedExchange(exchange);
        verify(restrictionRepository, never()).save(any());
        verify(moderationLog).log(eq(moderator), eq(ModerationActionType.RESOLVE_DISPUTE), eq(ModerationTargetType.DISPUTE),
                eq(dispute.getId()), eq(reader), eq("Недостатньо доказів"), org.mockito.ArgumentMatchers.startsWith("Відхилено"));
        verify(notificationService).notify(eq(reader), eq(NotificationType.DISPUTE_RESOLVED), eq(dispute.getId()), anyString());
        verify(notificationService).notify(eq(owner), eq(NotificationType.DISPUTE_RESOLVED), eq(dispute.getId()), anyString());
    }

    @Test
    @DisplayName("якщо обмін уже не в спорі, повторно він не завершується")
    void exchangeNotDisputed() {
        Dispute dispute = openDispute(owner);
        disputeService.resolve(dispute.getId(), moderator.getId(),
                new ResolveDisputeRequest(DisputeStatus.RESOLVED_FAVOR_OTHER, null));
        verify(exchangeService, never()).finalizeReturnedExchange(any());
    }

    @Test
    @DisplayName("перше підтверджене порушення обмежує акаунт на 14 днів")
    void firstViolationRestricts() {
        Dispute dispute = openDispute(owner);
        when(restrictionRepository.findByUserId(reader.getId())).thenReturn(List.of());

        disputeService.resolve(dispute.getId(), moderator.getId(),
                new ResolveDisputeRequest(DisputeStatus.RESOLVED_FAVOR_FILER, "Підтверджено"));

        assertThat(reader.getStatus()).isEqualTo(UserStatus.RESTRICTED);
        assertThat(reader.getRestrictedUntil()).isAfter(LocalDateTime.now().plusDays(13));
        assertThat(reader.getBooksDamaged()).isEqualTo(1);
        ArgumentCaptor<Restriction> captor = ArgumentCaptor.forClass(Restriction.class);
        verify(restrictionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(RestrictionType.TEMPORARY);
        assertThat(captor.getValue().getDispute()).isEqualTo(dispute);
        verify(notificationService).notify(eq(reader), eq(NotificationType.ACCOUNT_RESTRICTED), isNull(),
                org.mockito.ArgumentMatchers.contains("14 днів"));
    }

    @Test
    @DisplayName("повторне порушення блокує акаунт назавжди")
    void repeatViolationBlocks() {
        Dispute dispute = openDispute(reader);
        when(restrictionRepository.findByUserId(owner.getId())).thenReturn(List.of(Restriction.builder().build()));

        disputeService.resolve(dispute.getId(), moderator.getId(),
                new ResolveDisputeRequest(DisputeStatus.RESOLVED_FAVOR_FILER, "Знову"));

        assertThat(owner.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(owner.getRestrictedUntil()).isNull();
        ArgumentCaptor<Restriction> captor = ArgumentCaptor.forClass(Restriction.class);
        verify(restrictionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(RestrictionType.PERMANENT);
        verify(notificationService).notify(eq(owner), eq(NotificationType.ACCOUNT_RESTRICTED), isNull(),
                org.mockito.ArgumentMatchers.contains("заблоковано"));
    }
}