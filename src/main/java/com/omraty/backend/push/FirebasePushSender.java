package com.omraty.backend.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    private final FirebaseMessaging firebaseMessaging;

    public FirebasePushSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void send(String fcmToken, String title, String body) {
        Message message =
                Message.builder()
                        .setToken(fcmToken)
                        .setNotification(
                                com.google.firebase.messaging.Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                        .build();
        try {
            firebaseMessaging.send(message);
        } catch (FirebaseMessagingException e) {
            // Jeton périmé/désinstallation, panne FCM... : la notification reste visible dans
            // l'app quoi qu'il arrive (voir NotificationService), on se contente de logger.
            log.warn("Échec de l'envoi du push FCM (token={})", fcmToken, e);
        }
    }
}
