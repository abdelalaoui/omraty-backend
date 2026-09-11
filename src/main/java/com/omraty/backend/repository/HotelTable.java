package com.omraty.backend.repository;

final class HotelTable {

    private HotelTable() {}

    static final String HOTEL_COLUMNS =
            "id, name, location, city, stars, price_per_night, distance_to_haram, image_url,"
                    + " website_url";

    static final String SELECT_ALL_HOTELS =
            "SELECT " + HOTEL_COLUMNS + " FROM hotel ORDER BY id ASC";

    static final String SELECT_HOTELS_BY_CITY =
            "SELECT " + HOTEL_COLUMNS + " FROM hotel WHERE city = ? ORDER BY id ASC";

    static final String SELECT_HOTEL_BY_ID = "SELECT " + HOTEL_COLUMNS + " FROM hotel WHERE id = ?";

    static final String INSERT_HOTEL =
            "INSERT INTO hotel (name, location, city, stars, price_per_night, distance_to_haram,"
                    + " image_url, website_url) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                    + HOTEL_COLUMNS;

    // Champs non fournis (null) : COALESCE garde la valeur existante, permet une mise à jour
    // partielle (ex : ne changer que le prix sans toucher au reste).
    static final String UPDATE_HOTEL =
            "UPDATE hotel SET name = COALESCE(?, name), location = COALESCE(?, location), city ="
                    + " COALESCE(?, city), stars = COALESCE(?, stars), price_per_night ="
                    + " COALESCE(?, price_per_night), distance_to_haram = COALESCE(?,"
                    + " distance_to_haram), image_url = COALESCE(?, image_url), website_url ="
                    + " COALESCE(?, website_url) WHERE id = ? RETURNING "
                    + HOTEL_COLUMNS;

    static final String DELETE_HOTEL = "DELETE FROM hotel WHERE id = ?";
}
