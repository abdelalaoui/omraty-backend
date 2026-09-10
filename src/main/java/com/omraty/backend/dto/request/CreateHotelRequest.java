package com.omraty.backend.dto.request;

import com.omraty.backend.entities.enums.HotelCity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** distanceToHaram est optionnelle (texte libre) ; les autres champs sont requis. */
public record CreateHotelRequest(
        @NotBlank(message = "Le nom est requis") String name,
        @NotBlank(message = "La ville affichée (location) est requise") String location,
        @NotNull(message = "La ville normalisée est requise (MECCA ou MEDINA)") HotelCity city,
        @NotNull(message = "Le nombre d'étoiles est requis") Integer stars,
        @NotNull(message = "Le prix par nuit est requis") BigDecimal pricePerNight,
        String distanceToHaram,
        @NotBlank(message = "L'URL de la photo est requise") String imageUrl,
        @NotBlank(message = "L'URL du site web est requise") String websiteUrl) {}
