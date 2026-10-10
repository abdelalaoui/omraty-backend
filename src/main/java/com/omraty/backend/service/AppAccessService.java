package com.omraty.backend.service;

import com.omraty.backend.dto.response.AppAccessResponse;
import com.omraty.backend.repository.AuthRepository;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Fermeture de l'app (voir migration V49) : tant que app_open vaut false, l'app n'affiche qu'un
 * écran "bientôt disponible" (compte à rebours vers app_opening_at) et AppAccessFilter refuse les
 * routes du compte (réserver, payer...) aux utilisateurs sans accès anticipé. L'ouverture est
 * toujours manuelle (app_open repassé à true par l'admin), jamais automatique à app_opening_at.
 */
@Service
public class AppAccessService {

    static final String APP_OPEN_KEY = "app_open";
    static final String OPENING_AT_KEY = "app_opening_at";
    static final String CLOSED_MESSAGE_AR_KEY = "app_closed_message_ar";
    static final String CLOSED_MESSAGE_FR_KEY = "app_closed_message_fr";
    static final String CLOSED_MESSAGE_EN_KEY = "app_closed_message_en";
    static final String ALLOWED_PHONES_KEY = "app_allowed_phones";

    private final AppSettingService appSettingService;
    private final AuthRepository authRepository;

    public AppAccessService(AppSettingService appSettingService, AuthRepository authRepository) {
        this.appSettingService = appSettingService;
        this.authRepository = authRepository;
    }

    public boolean isAppOpen() {
        return Boolean.parseBoolean(appSettingService.getSetting(APP_OPEN_KEY).value().trim());
    }

    /**
     * Accès anticipé quand l'app est fermée : numéro listé dans app_allowed_phones, ou numéro de
     * test (otp_test_phone_numbers, ex. compte de review Apple — une review sur une app fermée
     * serait sinon refusée). userId null (non connecté) = jamais.
     */
    public boolean hasEarlyAccess(UUID userId) {
        if (userId == null) {
            return false;
        }
        return authRepository
                .findById(userId)
                .filter(user -> user.deletedAt() == null)
                .map(
                        user ->
                                isListed(user.phone(), ALLOWED_PHONES_KEY)
                                        || isListed(
                                                user.phone(),
                                                AuthService.OTP_TEST_PHONE_NUMBERS_SETTING_KEY))
                .orElse(false);
    }

    /** userId : utilisateur connecté, ou null si la requête n'a pas de JWT valide. */
    public AppAccessResponse getAccessStatus(UUID userId) {
        boolean open = isAppOpen();
        return new AppAccessResponse(
                open,
                open || hasEarlyAccess(userId),
                parseOpeningAt(appSettingService.getSetting(OPENING_AT_KEY).value()),
                blankToNull(appSettingService.getSetting(CLOSED_MESSAGE_AR_KEY).value()),
                blankToNull(appSettingService.getSetting(CLOSED_MESSAGE_FR_KEY).value()),
                blankToNull(appSettingService.getSetting(CLOSED_MESSAGE_EN_KEY).value()));
    }

    private boolean isListed(String phone, String settingKey) {
        String raw = appSettingService.getSetting(settingKey).value();
        return Arrays.stream(raw.split(",")).map(String::trim).anyMatch(phone::equals);
    }

    /** Date mal saisie par l'admin : pas de compte à rebours plutôt qu'une erreur au démarrage. */
    private static OffsetDateTime parseOpeningAt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
