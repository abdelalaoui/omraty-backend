package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.omraty.backend.entities.DeviceToken;
import com.omraty.backend.entities.Notification;
import com.omraty.backend.entities.enums.Platform;
import com.omraty.backend.exception.NotificationException;
import com.omraty.backend.push.PushSender;
import com.omraty.backend.repository.DeviceTokenRepository;
import com.omraty.backend.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private NotificationRepository notificationRepository;
    @Mock private DeviceTokenRepository deviceTokenRepository;
    @Mock private PushSender pushSender;

    private NotificationService notificationService() {
        return new NotificationService(notificationRepository, deviceTokenRepository, pushSender);
    }

    private Notification notification(long id, boolean read) {
        return new Notification(id, USER_ID, "Titre", "Message", read, LocalDateTime.now());
    }

    @Test
    void create_withRegisteredToken_sendsPushWithTitleAndMessage() {
        Notification created = notification(1L, false);
        when(notificationRepository.insert(USER_ID, "Titre", "Message")).thenReturn(created);
        when(deviceTokenRepository.findByUserId(USER_ID))
                .thenReturn(
                        Optional.of(
                                new DeviceToken(
                                        USER_ID,
                                        "fcm-token",
                                        Platform.ANDROID,
                                        LocalDateTime.now())));

        Notification result = notificationService().create(USER_ID, "Titre", "Message");

        assertThat(result).isEqualTo(created);
        verify(pushSender).send("fcm-token", "Titre", "Message");
    }

    @Test
    void create_withoutRegisteredToken_stillCreatesNotificationButSkipsPush() {
        Notification created = notification(1L, false);
        when(notificationRepository.insert(USER_ID, "Titre", "Message")).thenReturn(created);
        when(deviceTokenRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        Notification result = notificationService().create(USER_ID, "Titre", "Message");

        assertThat(result).isEqualTo(created);
        verify(pushSender, never()).send(any(), any(), any());
    }

    @Test
    void markAsRead_byOwner_marksNotificationRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification(1L, false)));
        when(notificationRepository.updateRead(1L)).thenReturn(Optional.of(notification(1L, true)));

        Notification result = notificationService().markAsRead(USER_ID, 1L);

        assertThat(result.read()).isTrue();
    }

    @Test
    void markAsRead_byNonOwner_throwsNotFoundWithoutLeakingExistence() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification(1L, false)));

        assertThatThrownBy(() -> notificationService().markAsRead(UUID.randomUUID(), 1L))
                .isInstanceOf(NotificationException.NotificationNotFoundException.class);
        verify(notificationRepository, never()).updateRead(1L);
    }

    @Test
    void markAsRead_whenNotificationDoesNotExist_throwsNotFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService().markAsRead(USER_ID, 1L))
                .isInstanceOf(NotificationException.NotificationNotFoundException.class);
    }
}
