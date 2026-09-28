package com.omraty.backend.dto.response;

import java.math.BigDecimal;

public record PromoPackageTierResponse(long id, int type, int capacity, BigDecimal price) {}
