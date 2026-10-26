package com.omraty.backend.controller;

import com.omraty.backend.dto.request.BroadcastNotificationRequest;
import com.omraty.backend.dto.request.SendNotificationRequest;
import com.omraty.backend.dto.response.BroadcastNotificationResponse;
import com.omraty.backend.dto.response.NotificationResponse;
import com.omraty.backend.mapper.NotificationMapper;
import com.omraty.backend.service.CustomOfferService;
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
    private final CustomOfferService customOfferService;

    public AdminNotificationController(
            NotificationService notificationService, CustomOfferService customOfferService) {
        this.notificationService = notificationService;
        this.customOfferService = customOfferService;
    }

    /**
     * Notification simple (sans action) au client de ce numéro — ex. réponse à une demande reçue
     * sur WhatsApp. Pour lui proposer un paiement, voir AdminCustomOfferController.
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationResponse> sendToClient(
            @Valid @RequestBody SendNotificationRequest request) {
        return ResponseEntity.ok(
                NotificationMapper.toResponse(
                        customOfferService.notifyClient(
                                request.phone(), request.title(), request.message())));
    }

    @PostMapping("/broadcast")
    public ResponseEntity<BroadcastNotificationResponse> broadcast(
            @Valid @RequestBody BroadcastNotificationRequest request) {
        int recipientCount =
                notificationService.broadcastToAllUsers(request.title(), request.message());
        return ResponseEntity.ok(new BroadcastNotificationResponse(recipientCount));
    }
}
