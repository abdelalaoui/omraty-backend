package com.omraty.backend.controller;

import com.omraty.backend.dto.request.RegisterDeviceTokenRequest;
import com.omraty.backend.dto.response.NotificationResponse;
import com.omraty.backend.mapper.NotificationMapper;
import com.omraty.backend.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Notifications et jeton FCM du client connecté. Accessible à tout utilisateur authentifié. */
@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PutMapping("/users/me/device-token")
    public ResponseEntity<Void> registerDeviceToken(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        notificationService.registerDeviceToken(userId, request.fcmToken(), request.platform());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users/me/notifications")
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                NotificationMapper.toResponseList(
                        notificationService.getNotificationsForUser(userId)));
    }

    @PatchMapping("/users/me/notifications/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            @AuthenticationPrincipal UUID userId, @PathVariable long id) {
        return ResponseEntity.ok(
                NotificationMapper.toResponse(notificationService.markAsRead(userId, id)));
    }
}
