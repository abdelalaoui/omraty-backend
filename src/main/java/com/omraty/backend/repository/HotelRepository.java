package com.omraty.backend.repository;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.enums.HotelCity;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class HotelRepository {

    private static final RowMapper<Hotel> HOTEL_ROW_MAPPER =
            (rs, rowNum) ->
                    new Hotel(
                            rs.getLong("id"),
                            rs.getString("name"),
                            rs.getString("location"),
                            HotelCity.valueOf(rs.getString("city")),
                            rs.getInt("stars"),
                            rs.getBigDecimal("price_per_night"),
                            rs.getString("distance_to_haram"),
                            rs.getString("image_url"),
                            rs.getString("website_url"));

    private final JdbcTemplate jdbcTemplate;

    public HotelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Tous les hôtels, pour la liste complète (Omra → Hôtels). */
    public List<Hotel> findAll() {
        return jdbcTemplate.query(HotelTable.SELECT_ALL_HOTELS, HOTEL_ROW_MAPPER);
    }

    /** Hôtels d'une ville donnée, pour le parcours VIP (choix Mecque puis Médine séparément). */
    public List<Hotel> findByCity(HotelCity city) {
        return jdbcTemplate.query(HotelTable.SELECT_HOTELS_BY_CITY, HOTEL_ROW_MAPPER, city.name());
    }

    public Optional<Hotel> findById(long id) {
        return jdbcTemplate.query(HotelTable.SELECT_HOTEL_BY_ID, HOTEL_ROW_MAPPER, id).stream()
                .findFirst();
    }

    public Hotel insert(
            String name,
            String location,
            HotelCity city,
            int stars,
            BigDecimal pricePerNight,
            String distanceToHaram,
            String imageUrl,
            String websiteUrl) {
        return jdbcTemplate
                .query(
                        HotelTable.INSERT_HOTEL,
                        HOTEL_ROW_MAPPER,
                        name,
                        location,
                        city.name(),
                        stars,
                        pricePerNight,
                        distanceToHaram,
                        imageUrl,
                        websiteUrl)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Échec de la création de l'hôtel"));
    }

    public Optional<Hotel> update(
            long id,
            String name,
            String location,
            HotelCity city,
            Integer stars,
            BigDecimal pricePerNight,
            String distanceToHaram,
            String imageUrl,
            String websiteUrl) {
        return jdbcTemplate
                .query(
                        HotelTable.UPDATE_HOTEL,
                        HOTEL_ROW_MAPPER,
                        name,
                        location,
                        city == null ? null : city.name(),
                        stars,
                        pricePerNight,
                        distanceToHaram,
                        imageUrl,
                        websiteUrl,
                        id)
                .stream()
                .findFirst();
    }

    /**
     * @return true si un hôtel a été supprimé, false si aucun hôtel ne correspond à cet id.
     */
    public boolean deleteById(long id) {
        return jdbcTemplate.update(HotelTable.DELETE_HOTEL, id) > 0;
    }
}
