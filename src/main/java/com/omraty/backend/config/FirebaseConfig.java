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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initialise le SDK Admin Firebase pour l'envoi de push (voir FirebasePushSender). Credentials
 * résolus via la variable d'environnement FIREBASE_SERVICE_ACCOUNT_KEY (même principe que les
 * secrets AWS déjà en place) : soit le contenu JSON de la clé de compte de service directement,
 * soit un chemin vers le fichier JSON — jamais codé en dur ici.
 */
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseMessaging firebaseMessaging(
            @Value("${app.firebase.service-account-key:}") String serviceAccountKey)
            throws IOException {
        GoogleCredentials credentials = loadCredentials(serviceAccountKey);
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
        FirebaseApp app =
                FirebaseApp.getApps().isEmpty()
                        ? FirebaseApp.initializeApp(options)
                        : FirebaseApp.getInstance();
        return FirebaseMessaging.getInstance(app);
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
