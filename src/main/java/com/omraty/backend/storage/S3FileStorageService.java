package com.omraty.backend.storage;

import com.omraty.backend.exception.UserException;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stores files in an S3 bucket. Active when app.upload.provider=s3. Credentials are resolved via
 * the default AWS provider chain (AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY env vars, or an IAM role)
 * — never hardcode them here.
 *
 * <p>The returned value is the object key (not a browsable URL): the bucket is not meant to be
 * public since these are identity documents. Serving the photo back to a client will need a
 * dedicated endpoint that generates a short-lived presigned URL — left for a follow-up ticket.
 */
@Service
@ConditionalOnProperty(prefix = "app.upload", name = "provider", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final String bucket;

    public S3FileStorageService(
            @Value("${app.upload.s3.region}") String region,
            @Value("${app.upload.s3.bucket}") String bucket) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder().region(Region.of(region)).build();
    }

    @Override
    public String store(MultipartFile file, String subDir) {
        String key =
                subDir + "/" + UUID.randomUUID() + FileExtensions.of(file.getOriginalFilename());
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
        } catch (IOException e) {
            throw new UserException.PhotoStorageException(
                    "Échec de l'envoi de la photo vers S3", e);
        }
        return key;
    }
}
