package com.omraty.backend.repository;

import com.omraty.backend.entities.AppSetting;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AppSettingRepository {

    private static final RowMapper<AppSetting> APP_SETTING_ROW_MAPPER =
            (rs, rowNum) ->
                    new AppSetting(
                            rs.getString("key"),
                            rs.getString("value"),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public AppSettingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<AppSetting> findByKey(String key) {
        return jdbcTemplate
                .query(AppSettingTable.SELECT_APP_SETTING_BY_KEY, APP_SETTING_ROW_MAPPER, key)
                .stream()
                .findFirst();
    }

    /** Vide si key inconnue : les réglages sont seedés par migration, jamais créés à la volée. */
    public Optional<AppSetting> updateValue(String key, String value) {
        return jdbcTemplate
                .query(AppSettingTable.UPDATE_APP_SETTING_VALUE, APP_SETTING_ROW_MAPPER, value, key)
                .stream()
                .findFirst();
    }
}
