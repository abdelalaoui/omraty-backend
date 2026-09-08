package com.omraty.backend.repository;

import com.omraty.backend.entities.Banner;
import java.time.LocalDateTime;
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
                            rs.getObject("updated_at", LocalDateTime.class));

    private final JdbcTemplate jdbcTemplate;

    public BannerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<Banner> findBanner() {
        return jdbcTemplate.query(BannerTable.SELECT_BANNER, BANNER_ROW_MAPPER).stream()
                .findFirst();
    }

    public Optional<Banner> updateImage(String imageUrl, String title, String description) {
        return jdbcTemplate
                .query(
                        BannerTable.UPDATE_BANNER_IMAGE,
                        BANNER_ROW_MAPPER,
                        imageUrl,
                        title,
                        description)
                .stream()
                .findFirst();
    }

    public Optional<Banner> updateVisibility(boolean visible) {
        return jdbcTemplate
                .query(BannerTable.UPDATE_BANNER_VISIBILITY, BANNER_ROW_MAPPER, visible)
                .stream()
                .findFirst();
    }
}
