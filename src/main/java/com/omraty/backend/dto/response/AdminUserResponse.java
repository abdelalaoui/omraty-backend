package com.omraty.backend.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Un compte client dans la liste admin (GET /admin/users) : sans la photo d'identité, pour garder
 * la liste légère (voir GET /admin/identity-verifications pour la revue NNI/photo).
 */
public record AdminUserResponse(
        UUID id,
        String phone,
        String gender,
        String nni,
        boolean identityVerified,
        LocalDateTime createdAt) {}
