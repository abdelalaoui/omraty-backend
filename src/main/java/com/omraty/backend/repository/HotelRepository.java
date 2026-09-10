package com.omraty.backend.repository;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.enums.HotelCity;
import java.util.List;
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
}
