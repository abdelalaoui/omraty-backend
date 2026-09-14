package com.omraty.backend.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initialise le SDK Admin Firebase pour l'envoi de push (voir FirebasePushSender). Credentials
 * résolus via la variable d'environnement FIREBASE_SERVICE_ACCOUNT_KEY (même principe que les
 * secrets AWS déjà en place) : soit le contenu JSON de la clé de compte de service directement,
 * soit un chemin vers le fichier JSON — jamais codé en dur ici.
 *
 * <p>Les notifications push sont une fonctionnalité annexe : si la clé est absente ou invalide, on
 * logue l'erreur et on démarre sans bean FirebaseMessaging plutôt que de faire planter toute
 * l'application (voir FirebasePushSender, qui tolère son absence).
 */
@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Bean
    public FirebaseMessaging firebaseMessaging(
            @Value("${app.firebase.service-account-key:}") String serviceAccountKey) {
        try {
            GoogleCredentials credentials = loadCredentials(serviceAccountKey);
            FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
            FirebaseApp app =
                    FirebaseApp.getApps().isEmpty()
                            ? FirebaseApp.initializeApp(options)
                            : FirebaseApp.getInstance();
            return FirebaseMessaging.getInstance(app);
        } catch (Exception e) {
            log.error(
                    "Initialisation Firebase impossible (FIREBASE_SERVICE_ACCOUNT_KEY absent ou"
                            + " invalide) : les notifications push FCM sont désactivées, le reste"
                            + " de l'application démarre normalement.",
                    e);
            return null;
        }
    }

    private GoogleCredentials loadCredentials(String serviceAccountKey) throws IOException {
        if (serviceAccountKey == null || serviceAccountKey.isBlank()) {
            throw new IllegalStateException(
                    "FIREBASE_SERVICE_ACCOUNT_KEY doit être défini (contenu JSON de la clé de"
                            + " compte de service Firebase, ou chemin vers le fichier JSON)");
        }
        try (InputStream stream = openCredentialsStream(serviceAccountKey)) {
            return GoogleCredentials.fromStream(stream);
        }
    }

    private InputStream openCredentialsStream(String serviceAccountKey) throws IOException {
        // Une clé de compte de service JSON commence toujours par '{' : on distingue ainsi le
        // contenu JSON inline d'un chemin vers le fichier.
        if (serviceAccountKey.stripLeading().startsWith("{")) {
            return new ByteArrayInputStream(serviceAccountKey.getBytes(StandardCharsets.UTF_8));
        }
        return new FileInputStream(serviceAccountKey);
    }
}
