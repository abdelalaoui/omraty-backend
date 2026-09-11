package com.omraty.backend.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AgencyCodeResponse(
        long id,
        String agencyName,
        String phoneNumber,
        BigDecimal discountPercentage,
        String code,
        boolean used,
        UUID accountId,
        LocalDateTime createdAt) {}
