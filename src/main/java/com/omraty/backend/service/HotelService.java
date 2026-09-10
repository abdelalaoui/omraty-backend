package com.omraty.backend.service;

import com.omraty.backend.entities.Hotel;
import com.omraty.backend.entities.enums.HotelCity;
import com.omraty.backend.exception.HotelException;
import com.omraty.backend.repository.HotelRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class HotelService {

    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_LOCATION_LENGTH = 255;
    private static final int MAX_DISTANCE_TO_HARAM_LENGTH = 255;
    private static final int MAX_URL_LENGTH = 500;
    private static final int MIN_STARS = 1;
    private static final int MAX_STARS = 5;

    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    /** Hôtels à afficher : tous, ou filtrés par ville (city non fourni = liste complète). */
    public List<Hotel> getHotels(HotelCity city) {
        return city == null ? hotelRepository.findAll() : hotelRepository.findByCity(city);
    }

    /** Ajoute un nouvel hôtel. */
    public Hotel createHotel(
            String name,
            String location,
            HotelCity city,
            int stars,
            BigDecimal pricePerNight,
            String distanceToHaram,
            String imageUrl,
            String websiteUrl) {
        validateName(name);
        validateLocation(location);
        validateStars(stars);
        validatePricePerNight(pricePerNight);
        if (distanceToHaram != null) {
            validateDistanceToHaram(distanceToHaram);
        }
        validateImageUrl(imageUrl);
        validateWebsiteUrl(websiteUrl);
        return hotelRepository.insert(
                name, location, city, stars, pricePerNight, distanceToHaram, imageUrl, websiteUrl);
    }

    /**
     * Met à jour un hôtel existant. Tous les champs sont optionnels : seuls ceux fournis (non null)
     * sont modifiés, ce qui permet par ex. de ne changer que le prix par nuit sans toucher au
     * reste.
     */
    public Hotel updateHotel(
            long id,
            String name,
            String location,
            HotelCity city,
            Integer stars,
            BigDecimal pricePerNight,
            String distanceToHaram,
            String imageUrl,
            String websiteUrl) {
        if (name != null) {
            validateName(name);
        }
        if (location != null) {
            validateLocation(location);
        }
        if (stars != null) {
            validateStars(stars);
        }
        if (pricePerNight != null) {
            validatePricePerNight(pricePerNight);
        }
        if (distanceToHaram != null) {
            validateDistanceToHaram(distanceToHaram);
        }
        if (imageUrl != null) {
            validateImageUrl(imageUrl);
        }
        if (websiteUrl != null) {
            validateWebsiteUrl(websiteUrl);
        }
        return hotelRepository
                .update(
                        id,
                        name,
                        location,
                        city,
                        stars,
                        pricePerNight,
                        distanceToHaram,
                        imageUrl,
                        websiteUrl)
                .orElseThrow(
                        () ->
                                new HotelException.HotelNotFoundException(
                                        "Hôtel introuvable (id=" + id + ")"));
    }

    /** Supprime un hôtel. */
    public void deleteHotel(long id) {
        if (!hotelRepository.deleteById(id)) {
            throw new HotelException.HotelNotFoundException("Hôtel introuvable (id=" + id + ")");
        }
    }

    private void validateName(String name) {
        if (name.isBlank()) {
            throw new HotelException.InvalidHotelRequestException("Le nom ne peut pas être vide");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new HotelException.InvalidHotelRequestException(
                    "Le nom dépasse la longueur maximale autorisée (" + MAX_NAME_LENGTH + ")");
        }
    }

    private void validateLocation(String location) {
        if (location.isBlank()) {
            throw new HotelException.InvalidHotelRequestException(
                    "La ville affichée (location) ne peut pas être vide");
        }
        if (location.length() > MAX_LOCATION_LENGTH) {
            throw new HotelException.InvalidHotelRequestException(
                    "La ville affichée (location) dépasse la longueur maximale autorisée ("
                            + MAX_LOCATION_LENGTH
                            + ")");
        }
    }

    private void validateStars(int stars) {
        if (stars < MIN_STARS || stars > MAX_STARS) {
            throw new HotelException.InvalidHotelRequestException(
                    "Le nombre d'étoiles doit être entre " + MIN_STARS + " et " + MAX_STARS);
        }
    }

    private void validatePricePerNight(BigDecimal pricePerNight) {
        if (pricePerNight.compareTo(BigDecimal.ZERO) <= 0) {
            throw new HotelException.InvalidHotelRequestException(
                    "Le prix par nuit doit être positif");
        }
    }

    private void validateDistanceToHaram(String distanceToHaram) {
        if (distanceToHaram.length() > MAX_DISTANCE_TO_HARAM_LENGTH) {
            throw new HotelException.InvalidHotelRequestException(
                    "La distance au lieu saint dépasse la longueur maximale autorisée ("
                            + MAX_DISTANCE_TO_HARAM_LENGTH
                            + ")");
        }
    }

    private void validateImageUrl(String imageUrl) {
        if (imageUrl.isBlank()) {
            throw new HotelException.InvalidHotelRequestException(
                    "L'URL de la photo ne peut pas être vide");
        }
        if (imageUrl.length() > MAX_URL_LENGTH) {
            throw new HotelException.InvalidHotelRequestException(
                    "L'URL de la photo dépasse la longueur maximale autorisée ("
                            + MAX_URL_LENGTH
                            + ")");
        }
    }

    private void validateWebsiteUrl(String websiteUrl) {
        if (websiteUrl.isBlank()) {
            throw new HotelException.InvalidHotelRequestException(
                    "L'URL du site web ne peut pas être vide");
        }
        if (websiteUrl.length() > MAX_URL_LENGTH) {
            throw new HotelException.InvalidHotelRequestException(
                    "L'URL du site web dépasse la longueur maximale autorisée ("
                            + MAX_URL_LENGTH
                            + ")");
        }
    }
}
