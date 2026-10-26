package com.omraty.backend.service;

import com.omraty.backend.entities.FamilyMember;
import com.omraty.backend.entities.User;
import com.omraty.backend.exception.UserException;
import com.omraty.backend.repository.AuthRepository;
import com.omraty.backend.repository.FamilyMemberRepository;
import com.omraty.backend.storage.FileStorageService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Membres de la famille d'un utilisateur (parcours « للعائلة » de l'offre, voir migration V65) :
 * NNI et photo de pièce d'identité de chaque membre, vérifiés par un admin comme ceux de
 * l'utilisateur (voir UserService), et réunis avec eux dans la liste des participants à la tombola
 * (vue draw_participant).
 */
@Service
public class FamilyMemberService {

    private static final String IDENTITY_PHOTO_SUBDIR = "identity";

    /** Même format que l'app (10 chiffres, voir BeneficiaryInfoScreen). */
    private static final Pattern NNI_PATTERN = Pattern.compile("\\d{10}");

    private final FamilyMemberRepository familyMemberRepository;
    private final AuthRepository authRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    public FamilyMemberService(
            FamilyMemberRepository familyMemberRepository,
            AuthRepository authRepository,
            FileStorageService fileStorageService,
            NotificationService notificationService) {
        this.familyMemberRepository = familyMemberRepository;
        this.authRepository = authRepository;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
    }

    /**
     * Enregistre les membres saisis d'un coup (nnis[i] avec photos[i]), tout ou rien : chaque NNI
     * est vérifié avant tout stockage. Un NNI déjà enregistré comme membre de ce même compte (ex.
     * nouvelle réservation pour la même famille) est réutilisé tel quel, sans nouvelle photo ; déjà
     * pris par une autre personne (compte ou membre d'une autre famille) -> refus.
     *
     * @return les membres dans l'ordre de la saisie.
     */
    @Transactional
    public List<FamilyMember> addMembers(
            UUID userId, List<String> nnis, List<MultipartFile> photos) {
        if (nnis == null || nnis.isEmpty() || photos == null || nnis.size() != photos.size()) {
            throw new UserException.InvalidIdentityRequestException(
                    "Un NNI et une photo sont requis pour chaque membre");
        }
        User owner =
                authRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new UserException.UserNotFoundException(
                                                "Utilisateur introuvable"));

        List<String> cleaned = new ArrayList<>();
        List<Optional<FamilyMember>> existing = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < nnis.size(); i++) {
            String nni = nnis.get(i) == null ? "" : nnis.get(i).trim();
            if (!NNI_PATTERN.matcher(nni).matches()) {
                throw new UserException.InvalidIdentityRequestException(
                        "Le NNI du membre " + (i + 1) + " doit contenir exactement 10 chiffres");
            }
            if (!seen.add(nni)) {
                throw new UserException.InvalidIdentityRequestException(
                        "Le NNI " + nni + " est saisi deux fois");
            }
            if (nni.equals(owner.nni())) {
                throw new UserException.InvalidIdentityRequestException(
                        "Le NNI " + nni + " est le vôtre : ne saisissez que les autres membres");
            }
            Optional<FamilyMember> sameNni = familyMemberRepository.findByNni(nni);
            if (sameNni.filter(member -> !member.userId().equals(userId)).isPresent()
                    || authRepository.existsOtherActiveUserWithNni(nni, userId)) {
                throw new UserException.NniAlreadyUsedException(
                        "Le NNI " + nni + " est déjà enregistré pour une autre personne");
            }
            if (sameNni.isEmpty()) {
                UserService.validateIdentityPhoto(photos.get(i));
            }
            cleaned.add(nni);
            existing.add(sameNni);
        }

        List<FamilyMember> result = new ArrayList<>();
        for (int i = 0; i < cleaned.size(); i++) {
            Optional<FamilyMember> alreadyMine = existing.get(i);
            if (alreadyMine.isPresent()) {
                result.add(alreadyMine.get());
                continue;
            }
            String photoUrl = fileStorageService.store(photos.get(i), IDENTITY_PHOTO_SUBDIR);
            result.add(familyMemberRepository.insert(userId, cleaned.get(i), photoUrl));
        }
        return result;
    }

    public List<FamilyMember> listForUser(UUID userId) {
        return familyMemberRepository.findByUserId(userId);
    }

    /** Membres en attente de vérification par un admin. */
    public List<FamilyMember> listPending() {
        return familyMemberRepository.findPending();
    }

    /** Marque le NNI/la photo du membre comme vérifiés, et prévient le titulaire du compte. */
    public FamilyMember approve(long memberId) {
        FamilyMember member = requirePending(memberId);
        FamilyMember approved = familyMemberRepository.approve(memberId).orElseThrow();
        notificationService.create(
                member.userId(),
                "Identité d'un membre vérifiée",
                "L'identité du membre de votre famille (NNI " + member.nni() + ") a été vérifiée.");
        return approved;
    }

    /**
     * Rejette le membre : il est supprimé (comme un NNI/photo rejeté est effacé du compte, voir
     * UserService.rejectIdentity) et le titulaire est invité à le saisir de nouveau.
     */
    public void reject(long memberId) {
        FamilyMember member = requirePending(memberId);
        familyMemberRepository.deleteById(memberId);
        notificationService.create(
                member.userId(),
                "Vérification d'un membre rejetée",
                "L'identité du membre de votre famille (NNI "
                        + member.nni()
                        + ") a été rejetée. Merci de renvoyer son NNI et sa photo.");
    }

    private FamilyMember requirePending(long memberId) {
        FamilyMember member =
                familyMemberRepository
                        .findById(memberId)
                        .orElseThrow(
                                () ->
                                        new UserException.UserNotFoundException(
                                                "Membre introuvable (id=" + memberId + ")"));
        if (member.identityVerified()) {
            throw new UserException.IdentityNotPendingException(
                    "L'identité de ce membre est déjà vérifiée");
        }
        return member;
    }
}
