package com.booksharing.model.dispute;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.model.exchange.Exchange;
import com.booksharing.model.exchange.ExchangeRepository;
import com.booksharing.model.exchange.ExchangeStatus;
import com.booksharing.model.user.User;
import com.booksharing.model.user.UserRepository;
import com.booksharing.model.user.UserRole;
import com.booksharing.model.user.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ескалація при {@code RESOLVED_FAVOR_FILER}: перше підтверджене
 * порушення - {@code TEMPORARY}-обмеження на {@link #TEMPORARY_RESTRICTION_DAYS}
 * днів, повторне - {@code PERMANENT} (блокування без дати завершення).
 */
@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final long TEMPORARY_RESTRICTION_DAYS = 14;

    private final DisputeRepository disputeRepository;
    private final RestrictionRepository restrictionRepository;
    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final DisputeMapper disputeMapper;

    @Transactional
    public DisputeResponse fileDispute(UUID exchangeId, UUID currentUserId, CreateDisputeRequest request) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ResourceNotFoundException("Обмін не знайдено: " + exchangeId));
        User filedBy = requireParticipant(exchange, currentUserId);

        boolean alreadyOpen = disputeRepository.findByExchangeId(exchangeId).stream()
                .anyMatch(d -> d.getStatus() == DisputeStatus.OPEN);
        if (alreadyOpen) {
            throw new IllegalStateException("По цьому обміну вже є відкрита скарга");
        }

        Dispute dispute = Dispute.builder()
                .exchange(exchange)
                .filedBy(filedBy)
                .description(request.description())
                .status(DisputeStatus.OPEN)
                .build();
        dispute = disputeRepository.save(dispute);

        exchange.setStatus(ExchangeStatus.DISPUTED);
        exchangeRepository.save(exchange);

        return disputeMapper.toResponse(dispute);
    }

    public DisputeResponse getById(UUID id, UUID currentUserId) {
        Dispute dispute = findDisputeOrThrow(id);
        boolean isParticipant = dispute.getExchange().getOwner().getId().equals(currentUserId)
                || dispute.getExchange().getReader().getId().equals(currentUserId);
        boolean isModerator = userRepository.findById(currentUserId)
                .map(u -> u.getRole() == UserRole.MODERATOR || u.getRole() == UserRole.ADMIN)
                .orElse(false);
        if (!isParticipant && !isModerator) {
            throw new AccessDeniedException("Немає доступу до цієї скарги");
        }
        return disputeMapper.toResponse(dispute);
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public List<DisputeResponse> listOpenDisputes() {
        return disputeRepository.findByStatus(DisputeStatus.OPEN).stream()
                .map(disputeMapper::toResponse)
                .toList();
    }

    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    @Transactional
    public DisputeResponse resolve(UUID disputeId, UUID moderatorId, ResolveDisputeRequest request) {
        if (request.status() == DisputeStatus.OPEN) {
            throw new IllegalArgumentException("Рішення не може бути статусом OPEN");
        }

        Dispute dispute = findDisputeOrThrow(disputeId);
        if (dispute.getStatus() != DisputeStatus.OPEN) {
            throw new IllegalStateException("Цю скаргу вже розглянуто");
        }

        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Модератора не знайдено: " + moderatorId));

        dispute.setStatus(request.status());
        dispute.setResolutionComment(request.resolutionComment());
        dispute.setModerator(moderator);
        dispute.setResolvedAt(LocalDateTime.now());
        dispute = disputeRepository.save(dispute);

        Exchange exchange = dispute.getExchange();
        exchange.setStatus(ExchangeStatus.COMPLETED);
        if (exchange.getCompletedAt() == null) {
            exchange.setCompletedAt(LocalDateTime.now());
        }
        exchangeRepository.save(exchange);

        if (request.status() == DisputeStatus.RESOLVED_FAVOR_FILER) {
            applyViolation(dispute, exchange);
        }

        return disputeMapper.toResponse(dispute);
    }

    private void applyViolation(Dispute dispute, Exchange exchange) {
        boolean filedByOwner = dispute.getFiledBy().getId().equals(exchange.getOwner().getId());
        User violator = filedByOwner ? exchange.getReader() : exchange.getOwner();

        violator.setBooksDamaged(violator.getBooksDamaged() + 1);

        int priorRestrictions = restrictionRepository.findByUserId(violator.getId()).size();
        RestrictionType type = priorRestrictions == 0 ? RestrictionType.TEMPORARY : RestrictionType.PERMANENT;
        LocalDateTime endsAt = type == RestrictionType.TEMPORARY
                ? LocalDateTime.now().plusDays(TEMPORARY_RESTRICTION_DAYS)
                : null;

        violator.setStatus(type == RestrictionType.TEMPORARY ? UserStatus.RESTRICTED : UserStatus.BLOCKED);
        violator.setRestrictedUntil(endsAt);
        userRepository.save(violator);

        restrictionRepository.save(Restriction.builder()
                .user(violator)
                .type(type)
                .reason("Підтверджено скаргою: " + dispute.getDescription())
                .dispute(dispute)
                .endsAt(endsAt)
                .build());
    }

    private User requireParticipant(Exchange exchange, UUID currentUserId) {
        if (exchange.getOwner().getId().equals(currentUserId)) {
            return exchange.getOwner();
        }
        if (exchange.getReader().getId().equals(currentUserId)) {
            return exchange.getReader();
        }
        throw new AccessDeniedException("Ви не берете участі в цьому обміні");
    }

    private Dispute findDisputeOrThrow(UUID id) {
        return disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Скаргу не знайдено: " + id));
    }
}