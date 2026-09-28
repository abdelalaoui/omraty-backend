package com.omraty.backend.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record PromoPackageResponse(
        long id,
        String title,
        String description,
        LocalDateTime createdAt,
        List<PromoPackageTierResponse> tiers) {}
