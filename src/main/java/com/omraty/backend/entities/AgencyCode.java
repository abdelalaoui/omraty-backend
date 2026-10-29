package com.omraty.backend.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Code d'accès remis par l'admin à une agence contactée hors app (WhatsApp) : code généré par le
 * système pour le compte client dont phoneNumber est le numéro (seul ce compte peut le valider),
 * avec le NIF de l'agence (null pour les codes créés avant V70) ; système (jamais saisi par
 * l'admin), à usage unique. accountId n'est renseigné qu'une fois le code validé par le
 * propriétaire de l'agence (voir AgencyCodeService.verifyCode), moment où used passe à true et où
 * discountPercentage s'applique à ce compte.
 */
public record AgencyCode(
        long id,
        String agencyName,
        String phoneNumber,
        String nif,
        BigDecimal discountPercentage,
        String code,
        boolean used,
        UUID accountId,
        LocalDateTime createdAt) {}
