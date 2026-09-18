package com.omraty.backend.service;

import com.omraty.backend.entities.AppSetting;
import com.omraty.backend.exception.AppSettingException;
import com.omraty.backend.repository.AppSettingRepository;
import org.springframework.stereotype.Service;

/**
 * Réglages clé/valeur modifiables sans redéploiement (voir migration V28), consultables/modifiables
 * par l'admin via GET/PATCH /admin/settings/{key}.
 */
@Service
public class AppSettingService {

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
}
