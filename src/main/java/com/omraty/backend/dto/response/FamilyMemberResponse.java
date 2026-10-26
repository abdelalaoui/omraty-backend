package com.omraty.backend.dto.response;

/** Membre de famille vu par son titulaire (GET/POST /users/me/family-members). */
public record FamilyMemberResponse(long id, String nni, boolean identityVerified) {}
