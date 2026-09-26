package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.omraty.backend.dto.response.VersionCheckResponse;
import com.omraty.backend.entities.AppSetting;
import com.omraty.backend.exception.AppSettingException;
import com.omraty.backend.repository.AppSettingRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppSettingServiceTest {

    @Mock private AppSettingRepository appSettingRepository;

    private AppSettingService appSettingService() {
        return new AppSettingService(appSettingRepository);
    }

    private AppSetting setting(String key, String value) {
        return new AppSetting(key, value, LocalDateTime.now());
    }

    @Test
    void getSetting_whenUnknownKey_throwsException() {
        when(appSettingRepository.findByKey("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appSettingService().getSetting("unknown"))
                .isInstanceOf(AppSettingException.AppSettingNotFoundException.class);
    }

    /**
     * Tâche 19 : GET /app/version-check (accès public) doit toujours renvoyer les 4 valeurs, sans
     * jamais lever d'exception — garanti par le seed de migration (V37), pas par ce service.
     */
    @Test
    void getVersionCheckSettings_returnsAllFourSeededValues() {
        when(appSettingRepository.findByKey("min_supported_version"))
                .thenReturn(Optional.of(setting("min_supported_version", "0.1.0")));
        when(appSettingRepository.findByKey("latest_version"))
                .thenReturn(Optional.of(setting("latest_version", "0.2.0")));
        when(appSettingRepository.findByKey("store_url_ios"))
                .thenReturn(Optional.of(setting("store_url_ios", "https://apps.apple.com/app/x")));
        when(appSettingRepository.findByKey("store_url_android"))
                .thenReturn(
                        Optional.of(
                                setting(
                                        "store_url_android",
                                        "https://play.google.com/store/apps/details?id=x")));

        VersionCheckResponse result = appSettingService().getVersionCheckSettings();

        assertThat(result)
                .isEqualTo(
                        new VersionCheckResponse(
                                "0.1.0",
                                "0.2.0",
                                "https://apps.apple.com/app/x",
                                "https://play.google.com/store/apps/details?id=x"));
    }
}
