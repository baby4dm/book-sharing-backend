package com.booksharing.model.user;

import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code @AuthenticationPrincipal User currentUser} працює тут напряму,
 * без обгортки на кшталт {@code UserDetails}, бо саме повний {@link User}
 * кладеться в SecurityContext у {@code JwtAuthenticationFilter}.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public UserPublicProfileResponse getPublicProfile(@PathVariable UUID id) {
        return userService.getPublicProfile(id);
    }

    @GetMapping("/me")
    public UserMeResponse getMyProfile(@AuthenticationPrincipal User currentUser) {
        return userService.getMyProfile(currentUser);
    }

    @PatchMapping("/me")
    public UserMeResponse updateMyProfile(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return userService.updateMyProfile(currentUser.getId(), request);
    }
}