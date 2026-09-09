package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Liste des identifiants d'avantages dans l'ordre d'affichage souhaité (du premier au dernier).
 * L'ordre d'affichage de chaque avantage est déduit de sa position dans la liste.
 */
public record ReorderBenefitsRequest(
        @NotEmpty(message = "La liste des avantages à réordonner est requise")
                List<Long> orderedIds) {}
