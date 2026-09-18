package com.omraty.backend.service;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.exception.ServiceTierException;
import com.omraty.backend.repository.ServiceTierRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceTierService {

    private static final int MAX_LABEL_LENGTH = 255;
    private static final int MIN_CAPACITY = 1;

    private final ServiceTierRepository serviceTierRepository;

    public ServiceTierService(ServiceTierRepository serviceTierRepository) {
        this.serviceTierRepository = serviceTierRepository;
    }

    /**
     * Formules actives de la grille des services Omra (chambre double/triple/quintuple, VIP,
     * Agence, autres), triées par ordre d'affichage.
     */
    public List<ServiceTier> getActiveServiceTiers() {
        return serviceTierRepository.findActiveServiceTiers();
    }

    /**
     * Ajoute une nouvelle formule à la grille des services Omra. capacity n'est autorisé que pour
     * type = ROOM ; price aussi, et y est requis (remplace le montant codé en dur côté app) —
     * contrairement à capacity, qui reste optionnel même pour ROOM. displayOrder/visible/closed non
     * fournis = valeurs par défaut (0, visible, pas fermée) — extensible à une nouvelle formule
     * sans changement de code.
     */
    public ServiceTier createServiceTier(
            ServiceTierType type,
            Integer capacity,
            BigDecimal price,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible,
            Boolean closed) {
        validateLabel(labelFr, "labelFr");
        validateLabel(labelEn, "labelEn");
        validateLabel(labelAr, "labelAr");
        validateCapacity(type, capacity);
        validatePrice(type, price);
        if (type == ServiceTierType.ROOM && price == null) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le prix est requis pour une formule de type ROOM");
        }
        int resolvedDisplayOrder = displayOrder != null ? displayOrder : 0;
        boolean isVisible = visible == null || visible;
        boolean isClosed = closed != null && closed;
        return serviceTierRepository.insert(
                type,
                capacity,
                price,
                labelFr,
                labelEn,
                labelAr,
                resolvedDisplayOrder,
                isVisible,
                isClosed);
    }

    /**
     * Met à jour une formule existante. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — ex. ne fermer temporairement une formule (closed) ou la masquer
     * (visible) sans toucher au reste. Si type, capacity ou price est fourni, chaque couple est
     * validé avec l'état existant (capacity et price ne peuvent s'appliquer qu'à une formule de
     * type ROOM), pour ne pas valider chaque champ isolément et laisser passer une combinaison
     * incohérente.
     */
    public ServiceTier updateServiceTier(
            long id,
            ServiceTierType type,
            Integer capacity,
            BigDecimal price,
            String labelFr,
            String labelEn,
            String labelAr,
            Integer displayOrder,
            Boolean visible,
            Boolean closed) {
        ServiceTier existing = getServiceTierOrThrow(id);
        if (labelFr != null) {
            validateLabel(labelFr, "labelFr");
        }
        if (labelEn != null) {
            validateLabel(labelEn, "labelEn");
        }
        if (labelAr != null) {
            validateLabel(labelAr, "labelAr");
        }
        if (type != null || capacity != null) {
            ServiceTierType effectiveType = type != null ? type : existing.type();
            Integer effectiveCapacity = capacity != null ? capacity : existing.capacity();
            validateCapacity(effectiveType, effectiveCapacity);
        }
        if (type != null || price != null) {
            ServiceTierType effectiveType = type != null ? type : existing.type();
            BigDecimal effectivePrice = price != null ? price : existing.price();
            validatePrice(effectiveType, effectivePrice);
        }
        return serviceTierRepository
                .update(
                        id,
                        type,
                        capacity,
                        price,
                        labelFr,
                        labelEn,
                        labelAr,
                        displayOrder,
                        visible,
                        closed)
                .orElseThrow(
                        () ->
                                new ServiceTierException.ServiceTierNotFoundException(
                                        "Formule introuvable (id=" + id + ")"));
    }

    private ServiceTier getServiceTierOrThrow(long id) {
        return serviceTierRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new ServiceTierException.ServiceTierNotFoundException(
                                        "Formule introuvable (id=" + id + ")"));
    }

    private void validateLabel(String label, String fieldName) {
        if (label.isBlank()) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le champ " + fieldName + " ne peut pas être vide");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le champ "
                            + fieldName
                            + " dépasse la longueur maximale autorisée ("
                            + MAX_LABEL_LENGTH
                            + ")");
        }
    }

    private void validateCapacity(ServiceTierType type, Integer capacity) {
        if (capacity == null) {
            return;
        }
        if (type != ServiceTierType.ROOM) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "La capacité ne s'applique qu'aux formules de type ROOM");
        }
        if (capacity < MIN_CAPACITY) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "La capacité doit être positive");
        }
    }

    private void validatePrice(ServiceTierType type, BigDecimal price) {
        if (price == null) {
            return;
        }
        if (type != ServiceTierType.ROOM) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le prix ne s'applique qu'aux formules de type ROOM");
        }
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le prix doit être positif");
        }
    }
}
