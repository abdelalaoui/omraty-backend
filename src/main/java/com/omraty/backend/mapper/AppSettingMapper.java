package com.omraty.backend.mapper;

import com.omraty.backend.dto.response.AppSettingResponse;
import com.omraty.backend.entities.AppSetting;

public final class AppSettingMapper {

    private AppSettingMapper() {}

    public static AppSettingResponse toResponse(AppSetting setting) {
        return new AppSettingResponse(setting.key(), setting.value(), setting.updatedAt());
    }
}
