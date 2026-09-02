package com.booksharing.controller;

import com.booksharing.dto.res.NotificationResponse;
import com.booksharing.service.NotificationService;
import com.booksharing.entity.User;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getMyNotifications(@AuthenticationPrincipal User currentUser) {
        return notificationService.getMyNotifications(currentUser.getId());
    }

    @PatchMapping("/{id}/read")
    public void markRead(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        notificationService.markRead(id, currentUser.getId());
    }
}