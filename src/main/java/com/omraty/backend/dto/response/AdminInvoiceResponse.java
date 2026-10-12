package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

/**
 * GET /admin/payments/{id}/invoice et POST .../invoice/regenerate : downloadUrl est une URL
 * présignée de courte durée (même mécanisme que GET /users/me/purchases/{id}/invoice, voir
 * FileStorageService.generatePresignedUrl) — à récupérer à nouveau si le lien a expiré, jamais
 * stockée côté panel.
 */
public record AdminInvoiceResponse(
        String invoiceNumber, LocalDateTime generatedAt, String downloadUrl) {}
