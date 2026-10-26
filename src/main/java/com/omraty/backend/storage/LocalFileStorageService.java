package com.omraty.backend.storage;

import com.omraty.backend.exception.UserException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Active by default (dev/tests). Set app.upload.provider=s3 to use {@link S3FileStorageService}.
 */
@Service
@ConditionalOnProperty(
        prefix = "app.upload",
        name = "provider",
        havingValue = "local",
        matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final Path rootDir;
    private final String baseUrl;

    public LocalFileStorageService(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.base-url}") String baseUrl) {
        this.rootDir = Path.of(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
        try {
            Files.createDirectories(rootDir);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Impossible de créer le dossier d'upload : " + rootDir, e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        String filename = UUID.randomUUID() + FileExtensions.of(file.getOriginalFilename());
        Path targetDir = rootDir.resolve(subDir).normalize();
        try {
            Files.createDirectories(targetDir);
            file.transferTo(targetDir.resolve(filename));
        } catch (IOException e) {
            throw new UserException.PhotoStorageException(
                    "Échec de l'enregistrement de la photo", e);
        }
        return baseUrl + "/" + subDir + "/" + filename;
    }

    @Override
    public String store(byte[] content, String filename, String contentType, String subDir) {
        String storedFilename = UUID.randomUUID() + FileExtensions.of(filename);
        Path targetDir = rootDir.resolve(subDir).normalize();
        try {
            Files.createDirectories(targetDir);
            Files.write(targetDir.resolve(storedFilename), content);
        } catch (IOException e) {
            throw new UserException.PhotoStorageException(
                    "Échec de l'enregistrement du fichier", e);
        }
        return baseUrl + "/" + subDir + "/" + storedFilename;
    }

    @Override
    public void delete(String key) {
        // key = baseUrl + "/" + subDir + "/" + filename (voir store) : on ne supprime que sous
        // rootDir.
        if (key == null || !key.startsWith(baseUrl + "/")) {
            return;
        }
        Path target = rootDir.resolve(key.substring(baseUrl.length() + 1)).normalize();
        if (!target.startsWith(rootDir)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Échec de la suppression du fichier {}", target, e);
        }
    }

    @Override
    public String generatePresignedUrl(String key) {
        // Local storage serves files directly from baseUrl; store() already returns a usable URL.
        return key;
    }
}
