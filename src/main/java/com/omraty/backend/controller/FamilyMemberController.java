package com.omraty.backend.controller;

import com.omraty.backend.dto.response.FamilyMemberResponse;
import com.omraty.backend.entities.FamilyMember;
import com.omraty.backend.service.FamilyMemberService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Membres de la famille de l'utilisateur connecté (parcours « للعائلة » de l'offre, voir
 * FamilyMemberService).
 */
@RestController
@RequestMapping("/users/me/family-members")
public class FamilyMemberController {

    private final FamilyMemberService familyMemberService;

    public FamilyMemberController(FamilyMemberService familyMemberService) {
        this.familyMemberService = familyMemberService;
    }

    @GetMapping
    public ResponseEntity<List<FamilyMemberResponse>> listMembers(
            @AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(toResponses(familyMemberService.listForUser(userId)));
    }

    /**
     * Ajout de plusieurs membres d'un coup (multipart) : un champ « nni » et un champ « photo » par
     * membre, dans le même ordre. Tout ou rien : un NNI invalide ou déjà pris par une autre
     * personne refuse toute la saisie.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FamilyMemberResponse>> addMembers(
            @AuthenticationPrincipal UUID userId,
            @RequestParam("nni") List<String> nnis,
            @RequestParam("photo") List<MultipartFile> photos) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponses(familyMemberService.addMembers(userId, nnis, photos)));
    }

    private static List<FamilyMemberResponse> toResponses(List<FamilyMember> members) {
        return members.stream()
                .map(
                        member ->
                                new FamilyMemberResponse(
                                        member.id(), member.nni(), member.identityVerified()))
                .toList();
    }
}
