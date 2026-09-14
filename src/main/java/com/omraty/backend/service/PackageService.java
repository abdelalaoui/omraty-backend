package com.omraty.backend.service;

import com.omraty.backend.entities.OmraPackage;
import com.omraty.backend.exception.PackageException;
import com.omraty.backend.repository.PackageRepository;
import java.time.LocalDate;
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

    public OmraPackage createPackage(
            String label, int groupSize, LocalDate startDate, LocalDate endDate) {
        validateLabel(label);
        validateGroupSize(groupSize);
        if (startDate == null || endDate == null) {
            throw new PackageException.InvalidPackageRequestException(
                    "La startDate et la endDate sont requises");
        }
        validateDateRange(startDate, endDate);
        return packageRepository.insert(label, groupSize, startDate, endDate);
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
     * Met à jour un package existant. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — permet notamment de renseigner startDate/endDate après coup sur un
     * package qui n'en a pas encore, sans toucher au reste. Si l'une des deux dates est fournie,
     * l'intervalle résultant (fourni ou existant) est validé pour ne pas laisser passer une
     * combinaison incohérente (ex. ne changer que endDate et se retrouver avant l'ancienne
     * startDate).
     */
    public OmraPackage updatePackage(
            long id, String label, Integer groupSize, LocalDate startDate, LocalDate endDate) {
        OmraPackage existing = getPackageById(id);
        if (label != null) {
            validateLabel(label);
        }
        if (groupSize != null) {
            validateGroupSize(groupSize);
        }
        if (startDate != null || endDate != null) {
            LocalDate effectiveStartDate = startDate != null ? startDate : existing.startDate();
            LocalDate effectiveEndDate = endDate != null ? endDate : existing.endDate();
            if (effectiveStartDate != null && effectiveEndDate != null) {
                validateDateRange(effectiveStartDate, effectiveEndDate);
            }
        }
        return packageRepository
                .update(id, label, groupSize, startDate, endDate)
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

    private void validateGroupSize(int groupSize) {
        if (groupSize <= 0) {
            throw new PackageException.InvalidPackageRequestException(
                    "Le groupSize doit être positif");
        }
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new PackageException.InvalidPackageRequestException(
                    "La endDate ne peut pas être avant la startDate");
        }
    }
}
