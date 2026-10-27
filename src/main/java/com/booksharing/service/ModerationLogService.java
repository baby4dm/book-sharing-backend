package com.booksharing.service;

import com.booksharing.entity.ModerationAction;
import com.booksharing.entity.User;
import com.booksharing.enums.ModerationActionType;
import com.booksharing.enums.ModerationTargetType;
import com.booksharing.repository.ModerationActionRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ModerationLogService {

    private static final int DETAILS_MAX = 300;

    private final ModerationActionRepository moderationActionRepository;

    @Transactional
    public void log(User moderator, ModerationActionType action, ModerationTargetType targetType,
                    UUID targetId, User targetUser, String reason, String details) {
        moderationActionRepository.save(ModerationAction.builder()
                .moderator(moderator)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .targetUser(targetUser)
                .reason(reason)
                .details(excerpt(details))
                .build());
    }

    static String excerpt(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.strip().replaceAll("\\s+", " ");
        return trimmed.length() <= DETAILS_MAX ? trimmed : trimmed.substring(0, DETAILS_MAX - 1) + "…";
    }
}
