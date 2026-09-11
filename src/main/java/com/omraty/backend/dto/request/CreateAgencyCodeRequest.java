package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Le code d'accès n'est pas saisi ici : il est généré par le système (voir AgencyCodeService). */
public record CreateAgencyCodeRequest(
        @NotBlank(message = "Le nom de l'agence est requis") String agencyName,
        @NotBlank(message = "Le numéro de téléphone est requis") String phoneNumber,
        @NotNull(message = "Le pourcentage de réduction est requis")
                BigDecimal discountPercentage) {}
