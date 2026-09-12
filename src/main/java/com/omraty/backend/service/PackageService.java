package com.omraty.backend.service;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.PackageRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PackageService {

    private static final int MAX_LABEL_LENGTH = 255;

    private final PackageRepository packageRepository;
    private final PackageCapacityService packageCapacityService;

    public PackageService(
            PackageRepository packageRepository, PackageCapacityService packageCapacityService) {
        this.packageRepository = packageRepository;
        this.packageCapacityService = packageCapacityService;
    }

    public List<OmraPackage> getPackages() {
        return packageRepository.findAll();
    }

    public OmraPackage createPackage(String label, int groupSize) {
        validateLabel(label);
        if (groupSize <= 0) {
            throw new PackageException.InvalidPackageRequestException(
                    "Le groupSize doit être positif");
        }
        return packageRepository.insert(label, groupSize);
    }

    /**
     * @throws PackageException.PackageNotFoundException si le package n'existe pas.
     */
    public OmraPackage getPackageById(long id) {
        return packageRepository
                .findById(id)
                .orElseThrow(
                        () ->
                                new PackageException.PackageNotFoundException(
                                        "Package introuvable (id=" + id + ")"));
    }

    /**
     * Tous les packages (périodes de départ), pleins ou non — aucun filtrage ici, voir
     * ReservationGroup. Pour l'utilisateur qui doit choisir sa période avant de réserver une
     * chambre (voir GET /reservation-groups). reservedSeats reflète le même plafond partagé que
     * PackageCapacityService (chambres + demandes VIP actives), pour éviter d'afficher un groupe
     * comme disponible alors qu'il est déjà plein une fois les VIP comptés.
     */
    public List<ReservationGroup> getReservationGroups() {
        return packageRepository.findAll().stream()
                .map(
                        pkg ->
                                new ReservationGroup(
                                        pkg, packageCapacityService.committedSeats(pkg.id())))
                .toList();
    }

    private void validateLabel(String label) {
        if (label.isBlank()) {
            throw new PackageException.InvalidPackageRequestException(
                    "Le label ne peut pas être vide");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new PackageException.InvalidPackageRequestException(
                    "Le label dépasse la longueur maximale autorisée (" + MAX_LABEL_LENGTH + ")");
        }
    }
}
