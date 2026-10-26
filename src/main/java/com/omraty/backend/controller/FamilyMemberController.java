package com.omraty.backend.controller;

import com.omraty.backend.dto.request.ValidateFamilyMembersRequest;
import com.omraty.backend.dto.response.FamilyMemberResponse;
import com.omraty.backend.service.FamilyMemberService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Membres de la famille de l'utilisateur connecté (parcours « للعائلة » de l'offre, voir
 * FamilyMemberService). Ils sont enregistrés avec la réservation de la famille (POST
 * /rooms/offer/family/reserve), pas ici.
 */
@RestController
@RequestMapping("/users/me/family-members")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    public FamilyMemberController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    /** Membres actifs (réservation payée). */
    @GetMapping
    public ResponseEntity<List<FamilyMemberResponse>> listMembers(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(
                familyMemberService.listForUser(userId).stream()
                        .map(
                                member ->
                                        new FamilyMemberResponse(
                                                member.id(),
                                                member.nni(),
                                                member.identityVerified()))
                        .toList());
    }

    /**
     * Vérifie les NNI saisis sans rien enregistrer (10 chiffres, pas de doublon, pas son propre
     * NNI, pas déjà pris par une autre personne) : 204 si tout est bon, sinon 400/409 avec le
     * message.
     */
    @PostMapping("/validate")
    public ResponseEntity<Void> validateMembers(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody ValidateFamilyMembersRequest request) {
        familyMemberService.validateNnis(userId, request.nnis());
        return ResponseEntity.noContent().build();
    }
}
