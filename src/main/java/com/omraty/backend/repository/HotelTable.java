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
}
