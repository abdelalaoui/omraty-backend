package com.omraty.backend.service;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.entities.enums.ServiceTierType;
import com.omraty.backend.exception.ServiceTierException;
import com.omraty.backend.repository.ServiceTierRepository;
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
     * type = ROOM ; displayOrder/visible/closed non fournis = valeurs par défaut (0, visible, pas
     * fermée) — extensible à une nouvelle formule sans changement de code.
     */
    public ServiceTier createServiceTier(
            ServiceTierType type,
            Integer capacity,
            String label,
            Integer displayOrder,
            Boolean visible,
            Boolean closed) {
        validateLabel(label);
        validateCapacity(type, capacity);
        int resolvedDisplayOrder = displayOrder != null ? displayOrder : 0;
        boolean isVisible = visible == null || visible;
        boolean isClosed = closed != null && closed;
        return serviceTierRepository.insert(
                type, capacity, label, resolvedDisplayOrder, isVisible, isClosed);
    }

    /**
     * Met à jour une formule existante. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — ex. ne fermer temporairement une formule (closed) ou la masquer
     * (visible) sans toucher au reste. Si type ou capacity est fourni, le couple est validé avec
     * l'état existant (capacity ne peut s'appliquer qu'à une formule de type ROOM), pour ne pas
     * valider chaque champ isolément et laisser passer une combinaison incohérente.
     */
    public ServiceTier updateServiceTier(
            long id,
            ServiceTierType type,
            Integer capacity,
            String label,
            Integer displayOrder,
            Boolean visible,
            Boolean closed) {
        ServiceTier existing = getServiceTierOrThrow(id);
        if (label != null) {
            validateLabel(label);
        }
        if (type != null || capacity != null) {
            ServiceTierType effectiveType = type != null ? type : existing.type();
            Integer effectiveCapacity = capacity != null ? capacity : existing.capacity();
            validateCapacity(effectiveType, effectiveCapacity);
        }
        return serviceTierRepository
                .update(id, type, capacity, label, displayOrder, visible, closed)
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

    private void validateLabel(String label) {
        if (label.isBlank()) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le label ne peut pas être vide");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new ServiceTierException.InvalidServiceTierRequestException(
                    "Le label dépasse la longueur maximale autorisée (" + MAX_LABEL_LENGTH + ")");
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
}
