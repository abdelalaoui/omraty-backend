package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminFamilyMemberResponse;
import com.omraty.backend.entities.FamilyMember;
import com.omraty.backend.entities.User;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.service.FamilyMemberService;
import com.omraty.backend.storage.FileStorageService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Réservé aux comptes ROLE_ADMIN (voir SecurityConfig) : revue des NNI/photos des membres de
 * famille, comme AdminIdentityController pour les comptes.
 */
@RestController
@RequestMapping("/admin/family-members")
public class AdminFamilyMemberController {

    private final FamilyMemberService familyMemberService;
    private final AuthRepository authRepository;
    private final FileStorageService fileStorageService;

    public AdminFamilyMemberController(
            FamilyMemberService familyMemberService,
            AuthRepository authRepository,
            FileStorageService fileStorageService) {
        this.familyMemberService = familyMemberService;
        this.authRepository = authRepository;
        this.fileStorageService = fileStorageService;
    }

    /** Membres en attente de vérification, avec la photo en URL présignée temporaire. */
    @GetMapping
    public ResponseEntity<List<AdminFamilyMemberResponse>> listPending() {
        List<FamilyMember> pending = familyMemberService.listPending();
        // Valeur = User entier : Collectors.toMap refuse les valeurs null (voir
        // RoomService.getAllPurchases, même raison).
        Map<UUID, User> ownersById =
                authRepository
                        .findByIds(pending.stream().map(FamilyMember::userId).distinct().toList())
                        .stream()
                        .collect(Collectors.toMap(User::id, user -> user));
        return ResponseEntity.ok(
                pending.stream()
                        .map(member -> toResponse(member, ownersById.get(member.userId())))
                        .toList());
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminFamilyMemberResponse> approve(@PathVariable long id) {
        FamilyMember member = familyMemberService.approve(id);
        return ResponseEntity.ok(
                toResponse(member, authRepository.findById(member.userId()).orElse(null)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable long id) {
        familyMemberService.reject(id);
        return ResponseEntity.noContent().build();
    }

    private AdminFamilyMemberResponse toResponse(FamilyMember member, User owner) {
        return new AdminFamilyMemberResponse(
                member.id(),
                member.userId(),
                owner == null ? null : owner.phone(),
                member.nni(),
                fileStorageService.generatePresignedUrl(member.idPhotoUrl()),
                member.identityVerified(),
                member.createdAt());
    }
}
