package com.omraty.backend.push;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushSender implements PushSender {

    private static final Logger log = LoggerFactory.getLogger(FirebasePushSender.class);

    private final ObjectProvider<FirebaseMessaging> firebaseMessaging;

    public FirebasePushSender(ObjectProvider<FirebaseMessaging> firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void send(String fcmToken, String title, String body) {
        // FirebaseConfig ne fournit pas de bean si FIREBASE_SERVICE_ACCOUNT_KEY est
        // absent/invalide (fonctionnalité annexe rendue non bloquante) : on se contente de
        // logger dans ce cas, la notification reste visible dans l'app (voir NotificationService).
        FirebaseMessaging messaging = firebaseMessaging.getIfAvailable();
        if (messaging == null) {
            log.warn(
                    "FCM non configuré (FIREBASE_SERVICE_ACCOUNT_KEY absent/invalide) : push non"
                            + " envoyé (token={})",
                    fcmToken);
            return;
        }
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
            messaging.send(message);
        } catch (FirebaseMessagingException e) {
            // Jeton périmé/désinstallation, panne FCM... : la notification reste visible dans
            // l'app quoi qu'il arrive (voir NotificationService), on se contente de logger.
            log.warn("Échec de l'envoi du push FCM (token={})", fcmToken, e);
        }
    }
}
