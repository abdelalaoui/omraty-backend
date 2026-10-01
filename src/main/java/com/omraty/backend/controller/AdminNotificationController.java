package com.omraty.backend.controller;

import com.omraty.backend.dto.request.BroadcastNotificationRequest;
import com.omraty.backend.dto.response.BroadcastNotificationResponse;
import com.omraty.backend.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Diffusion d'une notification à tous les utilisateurs (ex. annonce d'une offre, voir
 * NotificationService.broadcastToAllUsers) — réservé à ROLE_ADMIN (voir SecurityConfig, préfixe
 * /admin/**). Chaque destinataire reçoit la notification in-app immédiatement ; le push FCM n'est
 * tenté que pour ceux ayant un jeton enregistré, et silencieusement ignoré tant que FCM n'est pas
 * configuré (voir FirebaseConfig).
 */
@RestController
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    private final NotificationService notificationService;

    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastNotificationResponse> broadcast(
            @Valid @RequestBody BroadcastNotificationRequest request) {
        int recipientCount =
                notificationService.broadcastToAllUsers(request.title(), request.message());
        return ResponseEntity.ok(new BroadcastNotificationResponse(recipientCount));
    }
}
