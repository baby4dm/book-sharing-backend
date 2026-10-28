package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;

import com.booksharing.dto.req.UpdateUserProfileRequest;
import com.booksharing.dto.res.UserMeResponse;
import com.booksharing.dto.res.UserPublicProfileResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
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
    private final UserStatsService userStatsService;

    @Transactional(readOnly = true)
    public UserPublicProfileResponse getPublicProfile(UUID userId) {
        User user = findUserOrThrow(userId);
        UserPublicProfileResponse profile = userMapper.toPublicProfile(user);
        if (user.getRole() != UserRole.USER) {
            return staffProfile(profile);
        }
        return withStats(profile, userStatsService.statsFor(user));
    }

    @Transactional(readOnly = true)
    public UserMeResponse getMyProfile(User currentUser) {
        return withStats(userMapper.toMeResponse(currentUser), userStatsService.statsFor(currentUser));
    }

    @Transactional
    public UserMeResponse updateMyProfile(UUID currentUserId, UpdateUserProfileRequest request) {
        User user = findUserOrThrow(currentUserId);
        userMapper.updateEntityFromRequest(request, user);
        user = userRepository.save(user);
        return withStats(userMapper.toMeResponse(user), userStatsService.statsFor(user));
    }

    static UserPublicProfileResponse withStats(
            UserPublicProfileResponse p, UserStatsService.UserExchangeStats s) {
        return new UserPublicProfileResponse(
                p.id(), p.name(), p.avatarUrl(), p.settlementType(), p.region(), p.settlementName(),
                p.bio(), p.ratingAvg(),
                s.booksTaken(), s.booksReturnedOnTime(), s.booksOverdue(), s.booksDamaged(), s.booksGiven(),
                p.createdAt(), p.role());
    }

    static UserPublicProfileResponse staffProfile(UserPublicProfileResponse p) {
        return new UserPublicProfileResponse(
                p.id(), p.name(), p.avatarUrl(), null, null, null,
                p.bio(), null, null, null, null, null, null,
                p.createdAt(), p.role());
    }

    static UserMeResponse withStats(UserMeResponse p, UserStatsService.UserExchangeStats s) {
        return new UserMeResponse(
                p.id(), p.email(), p.name(), p.avatarUrl(), p.settlementType(), p.region(),
                p.settlementName(), p.bio(), p.ratingAvg(),
                s.booksTaken(), s.booksReturnedOnTime(), s.booksOverdue(), s.booksDamaged(), s.booksGiven(),
                p.role(), p.status(), p.restrictedUntil(), p.createdAt());
    }

    private User findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + userId));
    }
}