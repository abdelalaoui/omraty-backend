package com.omraty.backend.dto.response;

import java.util.UUID;

public record UserResponse(UUID id, String phone, String gender, boolean identityVerified) {}
