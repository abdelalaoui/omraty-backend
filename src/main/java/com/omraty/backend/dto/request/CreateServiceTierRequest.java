package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.ServiceTierType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * capacity n'est valide que pour type = ROOM (nombre de personnes). displayOrder, visible et closed
 * sont optionnels : par défaut la formule est ajoutée en fin de liste (displayOrder = 0), visible
 * et pas fermée. labelFr/labelEn/labelAr remplacent l'ancien label unique : GET /service-tiers
 * choisit celui de la langue demandée (header Accept-Language, voir ServiceTierController).
 */
public record CreateServiceTierRequest(
        @NotNull(message = "Le type est requis") ServiceTierType type,
        Integer capacity,
        @NotBlank(message = "Le label en français est requis") String labelFr,
        @NotBlank(message = "Le label en anglais est requis") String labelEn,
        @NotBlank(message = "Le label en arabe est requis") String labelAr,
        Integer displayOrder,
        Boolean visible,
        Boolean closed) {}
