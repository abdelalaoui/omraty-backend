package com.omraty.backend.dto.response;

/** Vue complète d'un avantage pour l'administration (icône/libellé/ordre/visibilité + id). */
public record AdminBenefitResponse(
        long id, String icon, String label, int displayOrder, boolean visible) {}
