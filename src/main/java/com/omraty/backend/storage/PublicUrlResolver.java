package com.omraty.backend.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Construit l'URL publique d'un fichier stocké via {@link FileStorageService}. En stockage S3,
 * {@link FileStorageService#store} ne renvoie que la clé de l'objet (le bucket n'est pas public par
 * défaut) ; les dossiers concernés (banner/, service-card/...) sont configurés publics en lecture
 * côté infra, donc on reconstruit ici l'URL complète à partir de la clé. En stockage local, la clé
 * est déjà une URL utilisable telle quelle.
 */
@Component
public class PublicUrlResolver {

    private static final String S3_PROVIDER = "s3";

    private final String uploadProvider;
    private final String s3Bucket;
    private final String s3Region;

    public PublicUrlResolver(
            @Value("${app.upload.provider}") String uploadProvider,
            @Value("${app.upload.s3.bucket}") String s3Bucket,
            @Value("${app.upload.s3.region}") String s3Region) {
        this.uploadProvider = uploadProvider;
        this.s3Bucket = s3Bucket;
        this.s3Region = s3Region;
    }

    public String toPublicUrl(String storedKey) {
        if (!S3_PROVIDER.equalsIgnoreCase(uploadProvider)) {
            return storedKey;
        }
        return "https://" + s3Bucket + ".s3." + s3Region + ".amazonaws.com/" + storedKey;
    }
}
