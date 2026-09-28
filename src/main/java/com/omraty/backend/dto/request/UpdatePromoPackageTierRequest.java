package com.omraty.backend.dto.request;

import java.math.BigDecimal;

/**
 * Mise à jour partielle : tous les champs sont optionnels, seuls ceux fournis (non null) sont
 * modifiés (les autres gardent leur valeur existante).
 */
public record UpdatePromoPackageTierRequest(Integer type, Integer capacity, BigDecimal price) {}
