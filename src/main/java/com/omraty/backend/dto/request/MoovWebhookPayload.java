package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Corps du webhook de confirmation envoyé par Moov (voir MoovWebhookController). DTO volontairement
 * minimal : le format exact du webhook Moov n'est pas encore connu côté banque — seuls
 * l'identifiant de transaction (clé pour retrouver l'achat, voir
 * booking_payment.moov_transaction_id, migration V33) et le statut sont nécessaires au traitement.
 * Les champs supplémentaires envoyés par Moov sont ignorés (comportement par défaut de Jackson) :
 * ce record s'ajustera à la doc reçue sans casser les appelants.
 */
public record MoovWebhookPayload(
        @NotBlank(message = "Le transactionId est requis") String transactionId,
        @NotBlank(message = "Le statut est requis") String status) {}
