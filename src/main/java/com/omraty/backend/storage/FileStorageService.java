package com.omraty.backend.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded files and returns a URL clients can use to retrieve them. The local disk
 * implementation is meant for development/early production; swap in an S3/Cloudinary implementation
 * later without touching callers.
 */
public interface FileStorageService {

    String store(MultipartFile file, String subDir);

    /**
     * Stocke un contenu généré côté serveur (voir InvoiceService, qui n'a pas de MultipartFile à
     * transmettre : le PDF est produit en mémoire, pas uploadé par un client).
     */
    String store(byte[] content, String filename, String contentType, String subDir);

    /**
     * Generates a short-lived, time-limited URL clients can use to fetch a previously stored file
     * that isn't publicly readable — e.g. identity photos. {@code key} is the value returned by
     * {@link #store}.
     */
    String generatePresignedUrl(String key);
}
