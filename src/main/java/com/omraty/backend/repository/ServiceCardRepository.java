package com.omraty.backend.repository;

import com.omraty.backend.entities.ServiceCard;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ServiceCardRepository {

    private static final RowMapper<ServiceCard> SERVICE_CARD_ROW_MAPPER =
            (rs, rowNum) ->
                    new ServiceCard(
                            rs.getLong("id"),
                            rs.getString("type"),
                            rs.getString("title_fr"),
                            rs.getString("title_en"),
                            rs.getString("title_ar"),
                            rs.getString("description_fr"),
                            rs.getString("description_en"),
                            rs.getString("description_ar"),
                            rs.getString("button_text_fr"),
                            rs.getString("button_text_en"),
                            rs.getString("button_text_ar"),
                            rs.getString("icon"),
                            rs.getString("image_url"),
                            rs.getBoolean("coming_soon"),
                            rs.getBoolean("visible"),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public ServiceCardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Cartes actives pour la home, avec leur contenu et leur état (comingSoon). */
    public List<ServiceCard> findActiveServiceCards() {
        return jdbcTemplate.query(
                ServiceCardTable.SELECT_ACTIVE_SERVICE_CARDS, SERVICE_CARD_ROW_MAPPER);
    }

    public Optional<ServiceCard> findById(long id) {
        return jdbcTemplate
                .query(ServiceCardTable.SELECT_SERVICE_CARD_BY_ID, SERVICE_CARD_ROW_MAPPER, id)
                .stream()
                .findFirst();
    }

    public ServiceCard insert(
            String type,
            String titleFr,
            String titleEn,
            String titleAr,
            String descriptionFr,
            String descriptionEn,
            String descriptionAr,
            String buttonTextFr,
            String buttonTextEn,
            String buttonTextAr,
            String icon,
            String imageUrl,
            boolean comingSoon,
            boolean visible) {
        return jdbcTemplate
                .query(
                        ServiceCardTable.INSERT_SERVICE_CARD,
                        SERVICE_CARD_ROW_MAPPER,
                        type,
                        titleFr,
                        titleEn,
                        titleAr,
                        descriptionFr,
                        descriptionEn,
                        descriptionAr,
                        buttonTextFr,
                        buttonTextEn,
                        buttonTextAr,
                        icon,
                        imageUrl,
                        comingSoon,
                        visible)
                .stream()
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Échec de la création de la carte de service"));
    }

    public Optional<ServiceCard> update(
            long id,
            String type,
            String titleFr,
            String titleEn,
            String titleAr,
            String descriptionFr,
            String descriptionEn,
            String descriptionAr,
            String buttonTextFr,
            String buttonTextEn,
            String buttonTextAr,
            String icon,
            String imageUrl,
            Boolean comingSoon,
            Boolean visible) {
        return jdbcTemplate
                .query(
                        ServiceCardTable.UPDATE_SERVICE_CARD,
                        SERVICE_CARD_ROW_MAPPER,
                        type,
                        titleFr,
                        titleEn,
                        titleAr,
                        descriptionFr,
                        descriptionEn,
                        descriptionAr,
                        buttonTextFr,
                        buttonTextEn,
                        buttonTextAr,
                        icon,
                        imageUrl,
                        comingSoon,
                        visible,
                        id)
                .stream()
                .findFirst();
    }

    /** Upload dédié (voir ServiceCardService.updateImage) : ne touche qu'à image_url. */
    public Optional<ServiceCard> updateImage(long id, String imageUrl) {
        return jdbcTemplate
                .query(
                        ServiceCardTable.UPDATE_SERVICE_CARD_IMAGE,
                        SERVICE_CARD_ROW_MAPPER,
                        imageUrl,
                        id)
                .stream()
                .findFirst();
    }
}
