package com.omraty.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** NNI des membres à vérifier avant la réservation (POST /users/me/family-members/validate). */
public record ValidateFamilyMembersRequest(
        @NotEmpty(message = "Au moins un membre est requis") List<String> nnis) {}
