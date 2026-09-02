package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import java.util.UUID;

import com.booksharing.dto.req.UpdateUserProfileRequest;
import com.booksharing.dto.res.UserMeResponse;
import com.booksharing.dto.res.UserPublicProfileResponse;
import com.booksharing.entity.User;
import com.booksharing.mapper.UserMapper;
import com.booksharing.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserPublicProfileResponse getPublicProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        return userMapper.toPublicProfile(user);
    }

    public UserMeResponse getMyProfile(User currentUser) {
        return userMapper.toMeResponse(currentUser);
    }

    /**
     * Приймає лише {@code currentUserId}, а не сам detached {@code User}
     * з {@code SecurityContext} — сутність там завантажена в окремій,
     * уже закритій транзакції фільтра, тому для оновлення її потрібно
     * перезавантажити тут, у власній транзакції сервісу.
     */
    @Transactional
    public UserMeResponse updateMyProfile(UUID currentUserId, UpdateUserProfileRequest request) {
        User user = findUserOrThrow(currentUserId);
        userMapper.updateEntityFromRequest(request, user);
        user = userRepository.save(user);
        return userMapper.toMeResponse(user);
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + userId));
    }
}