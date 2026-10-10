package com.omraty.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.omraty.backend.dto.response.AppAccessResponse;
import com.omraty.backend.entities.AppSetting;
import com.omraty.backend.entities.User;
import com.omraty.backend.repository.AuthRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppAccessServiceTest {

    private static final String PHONE = "+22240000001";

    @Mock private AppSettingService appSettingService;
    @Mock private AuthRepository authRepository;

    private AppAccessService appAccessService;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appAccessService = new AppAccessService(appSettingService, authRepository);
        setting(AppAccessService.APP_OPEN_KEY, "false");
        setting(AppAccessService.OPENING_AT_KEY, "");
        setting(AppAccessService.CLOSED_MESSAGE_AR_KEY, "");
        setting(AppAccessService.CLOSED_MESSAGE_FR_KEY, "");
        setting(AppAccessService.CLOSED_MESSAGE_EN_KEY, "");
        setting(AppAccessService.ALLOWED_PHONES_KEY, "");
        setting(AuthService.OTP_TEST_PHONE_NUMBERS_SETTING_KEY, "");
        lenient().when(authRepository.findById(userId)).thenReturn(Optional.of(user(null)));
    }

    private void setting(String key, String value) {
        lenient()
                .when(appSettingService.getSetting(key))
                .thenReturn(new AppSetting(key, value, LocalDateTime.now()));
    }

    private User user(LocalDateTime deletedAt) {
        return new User(
                userId,
                PHONE,
                null,
                "MALE",
                null,
                null,
                false,
                LocalDateTime.now(),
                "USER",
                deletedAt);
    }

    @Test
    void getAccessStatus_whenOpen_grantsEveryone() {
        setting(AppAccessService.APP_OPEN_KEY, "true");

        AppAccessResponse status = appAccessService.getAccessStatus(null);

        assertThat(status.appOpen()).isTrue();
        assertThat(status.accessGranted()).isTrue();
    }

    @Test
    void getAccessStatus_whenClosedAndAnonymous_deniesAccess() {
        AppAccessResponse status = appAccessService.getAccessStatus(null);

        assertThat(status.appOpen()).isFalse();
        assertThat(status.accessGranted()).isFalse();
    }

    @Test
    void getAccessStatus_whenClosedAndPhoneAllowed_grantsAccess() {
        setting(AppAccessService.ALLOWED_PHONES_KEY, "+22240000009, " + PHONE);

        assertThat(appAccessService.getAccessStatus(userId).accessGranted()).isTrue();
    }

    @Test
    void getAccessStatus_whenClosedAndTestPhone_grantsAccess() {
        setting(AuthService.OTP_TEST_PHONE_NUMBERS_SETTING_KEY, PHONE);

        assertThat(appAccessService.getAccessStatus(userId).accessGranted()).isTrue();
    }

    @Test
    void getAccessStatus_whenClosedAndPhoneNotListed_deniesAccess() {
        setting(AppAccessService.ALLOWED_PHONES_KEY, "+22240000009");

        assertThat(appAccessService.getAccessStatus(userId).accessGranted()).isFalse();
    }

    @Test
    void hasEarlyAccess_whenAccountDeleted_deniesAccess() {
        setting(AppAccessService.ALLOWED_PHONES_KEY, PHONE);
        when(authRepository.findById(userId)).thenReturn(Optional.of(user(LocalDateTime.now())));

        assertThat(appAccessService.hasEarlyAccess(userId)).isFalse();
    }

    @Test
    void getAccessStatus_parsesOpeningAtAndMessages() {
        setting(AppAccessService.OPENING_AT_KEY, "2026-11-01T20:00:00Z");
        setting(AppAccessService.CLOSED_MESSAGE_FR_KEY, "Ouverture des réservations");

        AppAccessResponse status = appAccessService.getAccessStatus(null);

        assertThat(status.openingAt()).isEqualTo(OffsetDateTime.parse("2026-11-01T20:00:00Z"));
        assertThat(status.closedMessageFr()).isEqualTo("Ouverture des réservations");
        assertThat(status.closedMessageAr()).isNull();
    }

    @Test
    void getAccessStatus_withInvalidOpeningAt_returnsNoCountdown() {
        setting(AppAccessService.OPENING_AT_KEY, "1er novembre");

        assertThat(appAccessService.getAccessStatus(null).openingAt()).isNull();
    }
}
