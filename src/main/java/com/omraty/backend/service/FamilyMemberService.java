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
 * Membres de la famille d'un utilisateur (parcours « للعائلة » de l'offre, voir migrations V65/V67)
 * : NNI et photo de pièce d'identité de chaque membre, vérifiés par un admin comme ceux de
 * l'utilisateur (voir UserService), et réunis avec eux dans la liste des participants à la tombola
 * (vue draw_participant). Les membres sont envoyés avec la réservation de la famille et restent en
 * attente de son paiement : activés à sa confirmation, supprimés (avec leurs photos) s'il expire ou
 * échoue — aucune demande non payée ne reste en base.
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
     * Vérifie les NNI saisis sans rien enregistrer (POST /users/me/family-members/validate), pour
     * signaler les erreurs avant le choix de l'hébergement et le paiement.
     */
    public void validateNnis(UUID userId, List<String> nnis) {
        checkNnis(userId, nnis);
    }

    /**
     * Enregistre les membres envoyés avec la réservation de paymentId (nnis[i] avec photos[i]),
     * tout ou rien, en attente de ce paiement. Un NNI déjà enregistré comme membre de ce compte est
     * réutilisé sans nouvelle photo (rattaché à ce paiement s'il était encore en attente d'un
     * autre) ; déjà pris par une autre personne (compte ou membre d'une autre famille) -> refus.
     *
     * @return les membres dans l'ordre de la saisie.
     */
    @Transactional
    public List<FamilyMember> addMembersForPayment(
            UUID userId, long paymentId, List<String> nnis, List<MultipartFile> photos) {
        if (photos == null || nnis == null || nnis.size() != photos.size()) {
            throw new UserException.InvalidIdentityRequestException(
                    "Un NNI et une photo sont requis pour chaque membre");
        }
        List<CheckedNni> checked = checkNnis(userId, nnis);
        for (int i = 0; i < checked.size(); i++) {
            if (checked.get(i).existing().isEmpty()) {
                UserService.validateIdentityPhoto(photos.get(i));
            }
        }
        List<FamilyMember> result = new ArrayList<>();
        for (int i = 0; i < checked.size(); i++) {
            CheckedNni nni = checked.get(i);
            Optional<FamilyMember> alreadyMine = nni.existing();
            if (alreadyMine.isPresent()) {
                FamilyMember member = alreadyMine.get();
                result.add(
                        member.pendingPaymentId() == null
                                ? member
                                : familyMemberRepository
                                        .attachToPendingPayment(member.id(), paymentId)
                                        .orElseThrow());
                continue;
            }
            String photoUrl = fileStorageService.store(photos.get(i), IDENTITY_PHOTO_SUBDIR);
            result.add(familyMemberRepository.insert(userId, nni.value(), photoUrl, paymentId));
        }
        return result;
    }

    /** Paiement de la réservation confirmé : ses membres deviennent actifs. */
    public void activateForPayment(long paymentId) {
        familyMemberRepository.activateForPayment(paymentId);
    }

    /**
     * Paiement de la réservation expiré ou échoué : ses membres encore en attente sont supprimés,
     * avec leur photo (voir PaymentExpirationService).
     */
    public void discardForPayment(long paymentId) {
        for (FamilyMember member : familyMemberRepository.findByPendingPayment(paymentId)) {
            familyMemberRepository.deleteById(member.id());
            fileStorageService.delete(member.idPhotoUrl());
        }
    }

    /** Membres actifs (payés) du compte. */
    public List<FamilyMember> listForUser(UUID userId) {
        return familyMemberRepository.findActiveByUserId(userId);
    }

    /** Membres actifs en attente de vérification par un admin. */
    public List<FamilyMember> listPending() {
        return familyMemberRepository.findPendingVerification();
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
        fileStorageService.delete(member.idPhotoUrl());
        notificationService.create(
                member.userId(),
                "Vérification d'un membre rejetée",
                "L'identité du membre de votre famille (NNI "
                        + member.nni()
                        + ") a été rejetée. Merci de renvoyer son NNI et sa photo.");
    }

    /** NNI validé, avec le membre de ce compte qui l'a déjà, le cas échéant. */
    private record CheckedNni(String value, Optional<FamilyMember> existing) {}

    private List<CheckedNni> checkNnis(UUID userId, List<String> nnis) {
        if (nnis == null || nnis.isEmpty()) {
            throw new UserException.InvalidIdentityRequestException(
                    "Au moins un membre est requis");
        }
        User owner =
                authRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new UserException.UserNotFoundException(
                                                "Utilisateur introuvable"));
        List<CheckedNni> checked = new ArrayList<>();
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
            checked.add(new CheckedNni(nni, sameNni));
        }
        return checked;
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
