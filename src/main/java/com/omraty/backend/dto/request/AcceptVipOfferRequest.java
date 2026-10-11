package com.omraty.backend.dto.request;

/**
 * Corps optionnel de POST /users/me/vip-requests/{id}/accept (voir VipRequestController) :
 * jusqu'ici sans body, désormais facultatif pour rester compatible avec les app 1.0/1.0.1, qui n'en
 * envoient aucun. paymentMethod : code d'un moyen de paiement actif (voir GET /payment-methods) ;
 * inconnu ou désactivé -> 400.
 */
public record AcceptVipOfferRequest(String paymentMethod) {}
