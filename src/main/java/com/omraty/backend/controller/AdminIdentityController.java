package com.omraty.backend.controller;

import com.omraty.backend.dto.response.UserResponse;
import com.omraty.backend.entities.User;
import com.omraty.backend.mapper.UserMapper;
import com.omraty.backend.service.UserService;
import com.omraty.backend.storage.FileStorageService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Réservé aux comptes ROLE_ADMIN (voir SecurityConfig) : revue des demandes NNI/photo. */
@RestController
@RequestMapping("/admin/identity-verifications")
public class AdminIdentityController {

    private final UserService userService;
    private final FileStorageService fileStorageService;

    public AdminIdentityController(UserService userService, FileStorageService fileStorageService) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listPending() {
        List<UserResponse> pending =
                userService.listPendingIdentityVerifications().stream()
                        .map(UserMapper::toResponse)
                        .map(this::withPresignedIdPhotoUrl)
                        .toList();
        return ResponseEntity.ok(pending);
    }

    /**
     * L'admin doit pouvoir visualiser la photo avant d'approuver/rejeter : la clé S3 brute n'est
     * pas exploitable telle quelle, on la remplace par une URL présignée temporaire.
     */
    private UserResponse withPresignedIdPhotoUrl(UserResponse response) {
        if (response.idPhotoUrl() == null) {
            return response;
        }
        return new UserResponse(
                response.id(),
                response.phone(),
                response.gender(),
                response.nni(),
                fileStorageService.generatePresignedUrl(response.idPhotoUrl()),
                response.identityVerified());
    }

    @PostMapping("/{userId}/approve")
    public ResponseEntity<UserResponse> approve(@PathVariable UUID userId) {
        User user = userService.approveIdentity(userId);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }

    @PostMapping("/{userId}/reject")
    public ResponseEntity<UserResponse> reject(@PathVariable UUID userId) {
        User user = userService.rejectIdentity(userId);
        return ResponseEntity.ok(UserMapper.toResponse(user));
    }
}
