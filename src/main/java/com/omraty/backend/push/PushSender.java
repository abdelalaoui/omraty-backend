package com.omraty.backend.push;

import java.util.Map;

/**
 * Envoi de notifications push. Voir {@link FirebasePushSender} pour l'implémentation Firebase Cloud
 * Messaging ; abstraction utile pour tester {@code NotificationService} sans dépendre de Firebase.
 */
public interface PushSender {

    /**
     * Envoie un push au jeton donné. Ne doit jamais lever d'exception vers l'appelant : un jeton
     * invalide/expiré ou une panne FCM ne doit pas empêcher la notification de rester visible en
     * base (voir NotificationService).
     */
    void send(String fcmToken, String title, String body);

    /**
     * Comme {@link #send(String, String, String)}, avec des données ajoutées au message (ex. action
     * et cible d'une notification, voir Notification) : l'app s'en sert quand on touche le push.
     */
    default void send(String fcmToken, String title, String body, Map<String, String> data) {
        send(fcmToken, title, body);
    }
}
