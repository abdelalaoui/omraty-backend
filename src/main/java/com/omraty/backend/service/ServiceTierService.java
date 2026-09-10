package com.omraty.backend.service;

import com.omraty.backend.entities.ServiceTier;
import com.omraty.backend.repository.ServiceTierRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceTierService {

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
}
