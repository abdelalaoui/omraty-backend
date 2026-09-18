package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.ServiceTierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * capacity et price ne sont valides que pour type = ROOM (capacity : nombre de personnes ; price :
 * montant réel de la formule). price y est requis (validé en service, pas ici, car conditionné au
 * type) — contrairement à capacity qui reste optionnel même pour ROOM. displayOrder, visible et
 * closed sont optionnels : par défaut la formule est ajoutée en fin de liste (displayOrder = 0),
 * visible et pas fermée. labelFr/labelEn/labelAr remplacent l'ancien label unique : GET
 * /service-tiers choisit celui de la langue demandée (header Accept-Language, voir
 * ServiceTierController).
 */
public record CreateServiceTierRequest(
        @NotNull(message = "Le type est requis") ServiceTierType type,
        Integer capacity,
        BigDecimal price,
        @NotBlank(message = "Le label en français est requis") String labelFr,
        @NotBlank(message = "Le label en anglais est requis") String labelEn,
        @NotBlank(message = "Le label en arabe est requis") String labelAr,
        Integer displayOrder,
        Boolean visible,
        Boolean closed) {}
