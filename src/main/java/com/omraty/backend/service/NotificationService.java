package com.omraty.backend.service;

import com.omraty.backend.entities.DeviceToken;
import com.omraty.backend.entities.Notification;
import com.omraty.backend.entities.enums.Platform;
import com.omraty.backend.exception.NotificationException;
import com.omraty.backend.push.PushSender;
import com.omraty.backend.repository.DeviceTokenRepository;
import com.omraty.backend.repository.NotificationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifications in-app + push FCM. Déclenchée depuis VipRequestService (approve/reject) et
 * UserService (approveIdentity/rejectIdentity) : voir {@link #create}.
 */
@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushSender pushSender;

    public NotificationService(
            NotificationRepository notificationRepository,
            DeviceTokenRepository deviceTokenRepository,
            PushSender pushSender) {
        this.notificationRepository = notificationRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushSender = pushSender;
    }

    /**
     * Insère la notification puis tente un push au jeton actif de l'utilisateur. Si l'utilisateur
     * n'a pas de jeton enregistré, l'envoi est ignoré silencieusement : la notification reste quand
     * même visible dans l'app.
     */
    @Transactional
    public Notification create(UUID userId, String title, String message) {
        Notification notification = notificationRepository.insert(userId, title, message);
        deviceTokenRepository
                .findByUserId(userId)
                .ifPresent(token -> pushSender.send(token.fcmToken(), title, message));
        return notification;
    }

    /** Enregistre le jeton FCM de l'utilisateur connecté, en remplaçant l'éventuel précédent. */
    public DeviceToken registerDeviceToken(UUID userId, String fcmToken, Platform platform) {
        return deviceTokenRepository.upsert(userId, fcmToken, platform);
    }

    /** Notifications du client connecté, les plus récentes d'abord. */
    public List<Notification> getNotificationsForUser(UUID userId) {
        return notificationRepository.findByUserId(userId);
    }

    /**
     * Marque une notification comme lue. Ownership vérifiée : une notification d'un autre
     * utilisateur est traitée comme introuvable (pas de fuite d'existence), comme
     * VipRequestService.accept.
     */
    @Transactional
    public Notification markAsRead(UUID userId, long id) {
        Notification notification =
                notificationRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new NotificationException.NotificationNotFoundException(
                                                "Notification introuvable (id=" + id + ")"));
        if (!notification.userId().equals(userId)) {
            throw new NotificationException.NotificationNotFoundException(
                    "Notification introuvable (id=" + id + ")");
        }
        if (notification.read()) {
            return notification;
        }
        return notificationRepository
                .updateRead(id)
                .orElseThrow(
                        () ->
                                new NotificationException.NotificationNotFoundException(
                                        "Notification introuvable (id=" + id + ")"));
    }
}
