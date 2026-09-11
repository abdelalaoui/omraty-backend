package com.omraty.backend.service;

import com.omraty.backend.entities.AgencyCode;
import com.omraty.backend.exception.AgencyCodeException;
import com.omraty.backend.repository.AgencyCodeRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Codes d'accès agence : quand un propriétaire d'agence contacte l'agence hors app (WhatsApp),
 * l'admin enregistre ses infos ({@link #createAgencyCode}) et le système génère un code unique que
 * l'admin lui transmet. Une fois ce code saisi côté client ({@link #verifyCode}), le compte est lié
 * et bénéficie de discountPercentage sur ses futures réservations.
 */
@Service
public class AgencyCodeService {

    // Alphabet sans caractères ambigus (0/O, 1/I) : le code est retransmis oralement ou recopié à
    // la main par l'admin vers l'agence.
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 8;
    private static final int MAX_GENERATION_ATTEMPTS = 10;

    private static final int MAX_AGENCY_NAME_LENGTH = 255;
    private static final int MAX_PHONE_NUMBER_LENGTH = 20;
    private static final BigDecimal MAX_DISCOUNT_PERCENTAGE = new BigDecimal("100");

    private final AgencyCodeRepository agencyCodeRepository;
    private final SecureRandom random = new SecureRandom();

    public AgencyCodeService(AgencyCodeRepository agencyCodeRepository) {
        this.agencyCodeRepository = agencyCodeRepository;
    }

    /**
     * L'admin enregistre l'agence ; le code n'est jamais saisi par l'admin, il est généré par le
     * système et renvoyé dans la réponse pour être transmis à l'agence.
     */
    public AgencyCode createAgencyCode(
            String agencyName, String phoneNumber, BigDecimal discountPercentage) {
        validateAgencyName(agencyName);
        validatePhoneNumber(phoneNumber);
        validateDiscountPercentage(discountPercentage);
        return agencyCodeRepository.insert(
                agencyName, phoneNumber, discountPercentage, generateUniqueCode());
    }

    /**
     * Le propriétaire de l'agence saisit le code reçu de l'admin : une fois validé, il est lié à
     * son compte (usage unique, voir agency_code.used). Le code est verrouillé avant vérification
     * pour empêcher deux comptes de valider le même code en même temps.
     *
     * @throws AgencyCodeException.AgencyCodeNotFoundException si le code n'existe pas.
     * @throws AgencyCodeException.AgencyCodeAlreadyUsedException s'il a déjà été validé.
     */
    @Transactional
    public AgencyCode verifyCode(UUID accountId, String code) {
        AgencyCode agencyCode =
                agencyCodeRepository
                        .findByCodeForUpdate(normalize(code))
                        .orElseThrow(
                                () ->
                                        new AgencyCodeException.AgencyCodeNotFoundException(
                                                "Code d'accès agence invalide"));
        if (agencyCode.used()) {
            throw new AgencyCodeException.AgencyCodeAlreadyUsedException(
                    "Ce code d'accès agence a déjà été utilisé");
        }
        return agencyCodeRepository.updateVerify(agencyCode.id(), accountId);
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String candidate = randomCode();
            if (!agencyCodeRepository.existsByCode(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Impossible de générer un code d'accès agence unique après "
                        + MAX_GENERATION_ATTEMPTS
                        + " tentatives");
    }

    private String randomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private String normalize(String code) {
        return code.trim().toUpperCase();
    }

    private void validateAgencyName(String agencyName) {
        if (agencyName.isBlank()) {
            throw new AgencyCodeException.InvalidAgencyCodeRequestException(
                    "Le nom de l'agence ne peut pas être vide");
        }
        if (agencyName.length() > MAX_AGENCY_NAME_LENGTH) {
            throw new AgencyCodeException.InvalidAgencyCodeRequestException(
                    "Le nom de l'agence dépasse la longueur maximale autorisée ("
                            + MAX_AGENCY_NAME_LENGTH
                            + ")");
        }
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isBlank()) {
            throw new AgencyCodeException.InvalidAgencyCodeRequestException(
                    "Le numéro de téléphone ne peut pas être vide");
        }
        if (phoneNumber.length() > MAX_PHONE_NUMBER_LENGTH) {
            throw new AgencyCodeException.InvalidAgencyCodeRequestException(
                    "Le numéro de téléphone dépasse la longueur maximale autorisée ("
                            + MAX_PHONE_NUMBER_LENGTH
                            + ")");
        }
    }

    private void validateDiscountPercentage(BigDecimal discountPercentage) {
        if (discountPercentage.compareTo(BigDecimal.ZERO) <= 0
                || discountPercentage.compareTo(MAX_DISCOUNT_PERCENTAGE) > 0) {
            throw new AgencyCodeException.InvalidAgencyCodeRequestException(
                    "Le pourcentage de réduction doit être compris entre 0 (exclu) et 100 (inclus)");
        }
    }
}
