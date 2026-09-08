package com.omraty.backend.controller;

import com.omraty.backend.dto.response.UserResponse;
import com.omraty.backend.entities.User;
import com.omraty.backend.mapper.UserMapper;
import com.omraty.backend.service.UserService;
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

    public AdminIdentityController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> listPending() {
        List<UserResponse> pending =
                userService.listPendingIdentityVerifications().stream()
                        .map(UserMapper::toResponse)
                        .toList();
        return ResponseEntity.ok(pending);
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
