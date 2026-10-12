package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/** POST /admin/payments/{id}/reject : motif transmis au client dans sa notification. */
public record RejectPaymentRequest(@NotBlank(message = "Le motif est requis") String reason) {}
