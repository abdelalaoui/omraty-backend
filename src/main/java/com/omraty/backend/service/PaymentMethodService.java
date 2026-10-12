package com.omraty.backend.service;

import com.omraty.backend.entities.PaymentMethod;
import com.omraty.backend.exception.PaymentMethodException;
import com.omraty.backend.repository.PaymentMethodRepository;
import com.omraty.backend.storage.FileStorageService;
import com.omraty.backend.storage.PublicUrlResolver;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Moyens de paiement manuels (Moov, Bankily, Masrvi, Sedad... voir migration V50, PaymentMethod) :
 * consultation (publique, voir GET /payment-methods) et gestion CRUD admin sans DELETE — un moyen
 * déjà utilisé par des paiements passés doit rester identifiable, on le désactive (enabled = false)
 * plutôt que de le supprimer (voir carte "Backend pour le panel admin").
 */
@Service
public class PaymentMethodService {

    private static final int MAX_CODE_LENGTH = 30;
    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_MERCHANT_NUMBER_LENGTH = 50;
    private static final int MAX_MERCHANT_NAME_LENGTH = 100;

    private static final String LOGO_SUBDIR = "payment-method";
    private static final long MAX_LOGO_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_LOGO_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final PaymentMethodRepository paymentMethodRepository;
    private final FileStorageService fileStorageService;
    private final PublicUrlResolver publicUrlResolver;

    public PaymentMethodService(
            PaymentMethodRepository paymentMethodRepository,
            FileStorageService fileStorageService,
            PublicUrlResolver publicUrlResolver) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.fileStorageService = fileStorageService;
        this.publicUrlResolver = publicUrlResolver;
    }

    /** Moyens de paiement actifs, triés par ordre d'affichage, pour GET /payment-methods. */
    public List<PaymentMethod> getEnabledPaymentMethods() {
        return paymentMethodRepository.findAllEnabled();
    }

    /** Tous les moyens de paiement (actifs + inactifs), pour GET /admin/payment-methods. */
    public List<PaymentMethod> getAllPaymentMethods() {
        return paymentMethodRepository.findAll();
    }

    /**
     * Moyen de paiement par son code, pour un affichage informatif (ex. numéro marchand dans GET
     * /users/me/purchases) — tolérant, vide si le code n'existe pas ou n'est plus actif plutôt que
     * de faire échouer tout l'affichage pour un achat déjà créé avec un moyen depuis désactivé.
     */
    public Optional<PaymentMethod> findByCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return paymentMethodRepository.findByCode(code);
    }

    /**
     * Moyen de paiement par son code, pour la création d'un paiement (voir BookingPaymentService) :
     * seul un moyen actif peut être choisi.
     *
     * @throws PaymentMethodException.UnknownOrDisabledPaymentMethodException si le code n'existe
     *     pas ou si le moyen est désactivé.
     */
    public PaymentMethod getEnabledOrThrow(String code) {
        return paymentMethodRepository
                .findByCode(code)
                .filter(PaymentMethod::enabled)
                .orElseThrow(
                        () ->
                                new PaymentMethodException.UnknownOrDisabledPaymentMethodException(
                                        "Moyen de paiement inconnu ou désactivé (code="
                                                + code
                                                + ")"));
    }

    /**
     * Ajoute un nouveau moyen de paiement. code est normalisé en majuscules et doit être unique ;
     * seuls nameFr et merchantNumber sont requis (nameEn/nameAr/instructions* restent optionnels,
     * voir PaymentMethodMapper pour le repli sur la variante fr). enabled non fourni = false, comme
     * le défaut de la colonne (voir migration V50) : un moyen nouvellement créé n'apparaît dans GET
     * /payment-methods qu'une fois explicitement activé.
     *
     * @throws PaymentMethodException.DuplicatePaymentMethodCodeException si le code est déjà pris.
     */
    public PaymentMethod createPaymentMethod(
            String code,
            String nameFr,
            String nameEn,
            String nameAr,
            String merchantNumber,
            String merchantName,
            String instructionsFr,
            String instructionsEn,
            String instructionsAr,
            Integer displayOrder,
            Boolean enabled) {
        String normalizedCode = validateAndNormalizeCode(code);
        validateRequired(nameFr, "nameFr", MAX_NAME_LENGTH);
        validateRequired(merchantNumber, "merchantNumber", MAX_MERCHANT_NUMBER_LENGTH);
        validateOptional(nameEn, "nameEn", MAX_NAME_LENGTH);
        validateOptional(nameAr, "nameAr", MAX_NAME_LENGTH);
        validateOptional(merchantName, "merchantName", MAX_MERCHANT_NAME_LENGTH);
        if (paymentMethodRepository.findByCode(normalizedCode).isPresent()) {
            throw new PaymentMethodException.DuplicatePaymentMethodCodeException(
                    "Un moyen de paiement avec ce code existe déjà (code=" + normalizedCode + ")");
        }
        return paymentMethodRepository.insert(
                normalizedCode,
                nameFr,
                nameEn,
                nameAr,
                merchantNumber,
                merchantName,
                instructionsFr,
                instructionsEn,
                instructionsAr,
                displayOrder != null ? displayOrder : 0,
                Boolean.TRUE.equals(enabled));
    }

    /**
     * Met à jour un moyen de paiement existant. Tous les champs sont optionnels : seuls ceux
     * fournis (non null) sont modifiés — ex. juste basculer enabled sans toucher au reste. code
     * n'est pas modifiable (absent de la liste, voir carte "Backend pour le panel admin") ; le logo
     * se change via {@link #updateLogo}.
     */
    public PaymentMethod updatePaymentMethod(
            long id,
            String nameFr,
            String nameEn,
            String nameAr,
            String merchantNumber,
            String merchantName,
            String instructionsFr,
            String instructionsEn,
            String instructionsAr,
            Boolean enabled,
            Integer displayOrder) {
        if (nameFr != null) {
            validateRequired(nameFr, "nameFr", MAX_NAME_LENGTH);
        }
        if (merchantNumber != null) {
            validateRequired(merchantNumber, "merchantNumber", MAX_MERCHANT_NUMBER_LENGTH);
        }
        validateOptional(nameEn, "nameEn", MAX_NAME_LENGTH);
        validateOptional(nameAr, "nameAr", MAX_NAME_LENGTH);
        validateOptional(merchantName, "merchantName", MAX_MERCHANT_NAME_LENGTH);
        return paymentMethodRepository
                .update(
                        id,
                        nameFr,
                        nameEn,
                        nameAr,
                        merchantNumber,
                        merchantName,
                        instructionsFr,
                        instructionsEn,
                        instructionsAr,
                        enabled,
                        displayOrder)
                .orElseThrow(
                        () ->
                                new PaymentMethodException.PaymentMethodNotFoundException(
                                        "Moyen de paiement introuvable (id=" + id + ")"));
    }

    /**
     * Upload admin du logo (même mécanisme S3/local que ServiceCardService.updateImage) : ne touche
     * qu'à logoUrl, le reste du contenu n'est pas modifié. toPublicUrl renvoie une URL complète
     * (voir PublicUrlResolver), jamais un chemin relatif.
     */
    public PaymentMethod updateLogo(long id, MultipartFile logo) {
        validateLogo(logo);
        String storedKey = fileStorageService.store(logo, LOGO_SUBDIR);
        String logoUrl = publicUrlResolver.toPublicUrl(storedKey);
        return paymentMethodRepository
                .updateLogo(id, logoUrl)
                .orElseThrow(
                        () ->
                                new PaymentMethodException.PaymentMethodNotFoundException(
                                        "Moyen de paiement introuvable (id=" + id + ")"));
    }

    private String validateAndNormalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le code est requis");
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() > MAX_CODE_LENGTH) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le code dépasse la longueur maximale autorisée (" + MAX_CODE_LENGTH + ")");
        }
        return normalized;
    }

    private void validateRequired(String value, String fieldName, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le champ " + fieldName + " est requis");
        }
        if (value.length() > maxLength) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + maxLength
                            + ")");
        }
    }

    private void validateOptional(String value, String fieldName, int maxLength) {
        if (value == null) {
            return;
        }
        if (value.isBlank()) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le champ " + fieldName + " ne peut pas être vide s'il est fourni");
        }
        if (value.length() > maxLength) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + maxLength
                            + ")");
        }
    }

    private void validateLogo(MultipartFile logo) {
        if (logo == null || logo.isEmpty()) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le logo est requis");
        }
        if (logo.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Le logo dépasse la taille maximale autorisée (5 Mo)");
        }
        String contentType = logo.getContentType();
        if (contentType == null || !ALLOWED_LOGO_CONTENT_TYPES.contains(contentType)) {
            throw new PaymentMethodException.InvalidPaymentMethodRequestException(
                    "Format de logo non supporté (JPEG ou PNG uniquement)");
        }
    }
}
