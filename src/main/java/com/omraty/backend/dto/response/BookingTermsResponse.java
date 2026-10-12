package com.omraty.backend.dto.response;

import java.util.List;

/**
 * GET /booking-terms (public) : version change dès qu'une condition est
 * ajoutée/modifiée/désactivée/réordonnée (voir BookingTermService#getCurrentVersion) — l'app la
 * renvoie dans acceptedTermsVersion à la réservation pour tracer ce qu'elle a affiché (voir
 * BookingPaymentService, carte "Conditions de réservation gérées depuis le serveur").
 */
public record BookingTermsResponse(String version, List<BookingTermItemResponse> terms) {}
