package com.omraty.backend.service;

import com.omraty.backend.entities.User;
import com.omraty.backend.exception.AuthException;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.storage.FileStorageService;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserService {

    private static final String IDENTITY_PHOTO_SUBDIR = "identity";
    private static final long MAX_PHOTO_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_PHOTO_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png");

    private final AuthRepository authRepository;
    private final FileStorageService fileStorageService;

    public UserService(AuthRepository authRepository, FileStorageService fileStorageService) {
        this.authRepository = authRepository;
        this.fileStorageService = fileStorageService;
    }

    public User updateIdentity(UUID userId, String nni, MultipartFile photo) {
        if (nni == null || nni.isBlank()) {
            throw new UserException.InvalidIdentityRequestException("Le NNI est requis");
        }
        validatePhoto(photo);

        String photoUrl = fileStorageService.store(photo, IDENTITY_PHOTO_SUBDIR);

        // identity_verified stays false: an admin must review the NNI/photo before marking the
        // account verified (see future admin verification endpoint).
        return authRepository
                .updateIdentity(userId, nni, photoUrl, false)
                .orElseThrow(
                        () ->
                                new AuthException.InvalidTokenException(
                                        "Utilisateur introuvable", null));
    }

    private void validatePhoto(MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new UserException.InvalidIdentityRequestException(
                    "La photo d'identité est requise");
        }
        if (photo.getSize() > MAX_PHOTO_SIZE_BYTES) {
            throw new UserException.InvalidIdentityRequestException(
                    "La photo dépasse la taille maximale autorisée (5 Mo)");
        }
        String contentType = photo.getContentType();
        if (contentType == null || !ALLOWED_PHOTO_CONTENT_TYPES.contains(contentType)) {
            throw new UserException.InvalidIdentityRequestException(
                    "Format de photo non supporté (JPEG ou PNG uniquement)");
        }
    }
}
