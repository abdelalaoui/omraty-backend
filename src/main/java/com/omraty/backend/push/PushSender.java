package com.omraty.backend.push;

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
}
