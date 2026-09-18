package com.omraty.backend.repository;

import com.omraty.backend.entities.Banner;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class BannerRepository {

    private static final RowMapper<Banner> BANNER_ROW_MAPPER =
            (rs, rowNum) ->
                    new Banner(
                            rs.getLong("id"),
                            rs.getString("image_url"),
                            rs.getString("title"),
                            rs.getString("description"),
                            rs.getBoolean("visible"),
                            rs.getInt("display_order"),
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BannerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Bannières visibles, triées par ordre d'affichage, pour l'écran d'accueil de l'app. */
    public List<Banner> findActiveBanners() {
        return jdbcTemplate.query(BannerTable.SELECT_ACTIVE_BANNERS, BANNER_ROW_MAPPER);
    }

    /** Toutes les bannières (visibles ou masquées), triées par ordre d'affichage, pour l'admin. */
    public List<Banner> findAllBanners() {
        return jdbcTemplate.query(BannerTable.SELECT_ALL_BANNERS, BANNER_ROW_MAPPER);
    }

    public Optional<Banner> findById(long id) {
        return jdbcTemplate.query(BannerTable.SELECT_BANNER_BY_ID, BANNER_ROW_MAPPER, id).stream()
                .findFirst();
    }

    /** Ordre d'affichage à utiliser pour une nouvelle bannière ajoutée en fin de liste. */
    public int nextDisplayOrder() {
        Integer max =
                jdbcTemplate.queryForObject(BannerTable.SELECT_MAX_DISPLAY_ORDER, Integer.class);
        return (max == null ? -1 : max) + 1;
    }

    public Banner insert(
            String imageUrl, String title, String description, int displayOrder, boolean visible) {
        return jdbcTemplate
                .query(
                        BannerTable.INSERT_BANNER,
                        BANNER_ROW_MAPPER,
                        imageUrl,
                        title,
                        description,
                        displayOrder,
                        visible)
                .stream()
                .findFirst()
                .orElseThrow(
                        () -> new IllegalStateException("Échec de la création de la bannière"));
    }

    public Optional<Banner> update(
            long id, String title, String description, Integer displayOrder, Boolean visible) {
        return jdbcTemplate
                .query(
                        BannerTable.UPDATE_BANNER,
                        BANNER_ROW_MAPPER,
                        title,
                        description,
                        displayOrder,
                        visible,
                        id)
                .stream()
                .findFirst();
    }

    public Optional<Banner> updateImage(long id, String imageUrl) {
        return jdbcTemplate
                .query(BannerTable.UPDATE_BANNER_IMAGE, BANNER_ROW_MAPPER, imageUrl, id)
                .stream()
                .findFirst();
    }

    public boolean deleteById(long id) {
        return jdbcTemplate.update(BannerTable.DELETE_BANNER, id) > 0;
    }
}
