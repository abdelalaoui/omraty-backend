package com.omraty.backend.entities;

import java.time.LocalDateTime;

/**
 * Moyen de paiement manuel proposé au client (Moov, Bankily, Masrvi, Sedad... voir migration V50) :
 * le client paie lui-même dans son wallet en utilisant merchantNumber et le code de paiement généré
 * pour son achat (voir PaymentReferenceGenerator), puis déclare avoir payé (« J'ai payé ») — aucune
 * intégration de passerelle, la confirmation reste manuelle côté admin.
 *
 * <p>nameFr/instructionsFr sont seuls requis ; nameEn/nameAr/instructionsEn/instructionsAr sont
 * nullables, avec repli sur la variante fr si absents (voir PaymentMethodMapper), même logique que
 * ServiceCard. enabled contrôle la présence dans GET /payment-methods ; displayOrder son ordre
 * d'affichage.
 */
public record PaymentMethod(
        long id,
        String code,
        String nameFr,
        String nameEn,
        String nameAr,
        String merchantNumber,
        String merchantName,
        String instructionsFr,
        String instructionsEn,
        String instructionsAr,
        String logoUrl,
        boolean enabled,
        int displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
