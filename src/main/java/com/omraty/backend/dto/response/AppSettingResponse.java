package com.omraty.backend.dto.response;

import java.time.LocalDateTime;

public record AppSettingResponse(String key, String value, LocalDateTime updatedAt) {}
