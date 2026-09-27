package com.omraty.backend.dto.response;

/**
 * Renvoyé par GET /users/me/purchases/{id}/invoice — URL présignée du PDF de facture (voir
 * BookingPaymentService.getInvoiceDownloadUrl), attendue par
 * CatalogRepository.getInvoiceDownloadUrl côté app.
 */
public record InvoiceDownloadResponse(String downloadUrl) {}
