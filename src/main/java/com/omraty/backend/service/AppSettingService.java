package com.omraty.backend.service;

import com.omraty.backend.dto.response.VersionCheckResponse;
import com.omraty.backend.entities.AppSetting;
import com.omraty.backend.exception.AppSettingException;
import com.omraty.backend.repository.AppSettingRepository;
import org.springframework.stereotype.Service;

/**
 * Réglages clé/valeur modifiables sans redéploiement (voir migration V28), consultables/modifiables
 * par l'admin via GET/PATCH /admin/settings/{key}. {@link #getVersionCheckSettings} expose en plus
 * 4 de ces réglages publiquement, sans authentification (voir migration V37, AppVersionController)
 * : l'app doit pouvoir vérifier une mise à jour avant que l'utilisateur soit connecté.
 */
@Service
public class AppSettingService {

    private static final String MIN_SUPPORTED_VERSION_KEY = "min_supported_version";
    private static final String LATEST_VERSION_KEY = "latest_version";
    private static final String STORE_URL_IOS_KEY = "store_url_ios";
    private static final String STORE_URL_ANDROID_KEY = "store_url_android";

    private final AppSettingRepository appSettingRepository;

    public AppSettingService(AppSettingRepository appSettingRepository) {
        this.appSettingRepository = appSettingRepository;
    }

    public AppSetting getSetting(String key) {
        return appSettingRepository
                .findByKey(key)
                .orElseThrow(
                        () ->
                                new AppSettingException.AppSettingNotFoundException(
                                        "Réglage introuvable (key=" + key + ")"));
    }

    /**
     * Valeur d'un réglage entier, ex. le délai de rappel en jours (voir PaymentReminderService).
     */
    public int getIntValue(String key) {
        return Integer.parseInt(getSetting(key).value());
    }

    public AppSetting updateSetting(String key, String value) {
        return appSettingRepository
                .updateValue(key, value)
                .orElseThrow(
                        () ->
                                new AppSettingException.AppSettingNotFoundException(
                                        "Réglage introuvable (key=" + key + ")"));
    }

    /**
     * Les 4 réglages consultés par l'app au démarrage, avant connexion (voir migration V37) :
     * seedés par migration, donc toujours présents — jamais d'AppSettingNotFoundException ici,
     * contrairement à {@link #getSetting}. Modifiables ensuite via PATCH /admin/settings/{key}
     * (déjà existant).
     */
    public VersionCheckResponse getVersionCheckSettings() {
        return new VersionCheckResponse(
                getSetting(MIN_SUPPORTED_VERSION_KEY).value(),
                getSetting(LATEST_VERSION_KEY).value(),
                getSetting(STORE_URL_IOS_KEY).value(),
                getSetting(STORE_URL_ANDROID_KEY).value());
    }
}
