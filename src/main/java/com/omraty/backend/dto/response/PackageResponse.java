package com.omraty.backend.dto.response;

import java.time.LocalDate;

public record PackageResponse(
        long id, String label, int groupSize, LocalDate startDate, LocalDate endDate) {}
