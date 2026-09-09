package com.omraty.backend.service;

import com.omraty.backend.entities.Benefit;
import com.omraty.backend.exception.BenefitException;
import com.omraty.backend.repository.BenefitRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class BenefitService {

    private static final int MAX_ICON_LENGTH = 100;
    private static final int MAX_LABEL_LENGTH = 255;

    private final BenefitRepository benefitRepository;

    public BenefitService(BenefitRepository benefitRepository) {
        this.benefitRepository = benefitRepository;
    }

    /** Avantages actifs à afficher sur la home, triés par ordre d'affichage. */
    public List<Benefit> getActiveBenefits() {
        return benefitRepository.findActiveBenefits();
    }

    /** Tous les avantages (visibles ou masqués), pour l'écran d'administration. */
    public List<Benefit> getAllBenefits() {
        return benefitRepository.findAllBenefits();
    }

    /**
     * Ajoute un nouvel avantage. displayOrder non fourni = ajouté en fin de liste ; visible non
     * fourni = visible par défaut.
     */
    public Benefit createBenefit(String icon, String label, Integer displayOrder, Boolean visible) {
        validateIcon(icon);
        validateLabel(label);
        int order = displayOrder != null ? displayOrder : benefitRepository.nextDisplayOrder();
        boolean isVisible = visible == null || visible;
        return benefitRepository.insert(icon, label, order, isVisible);
    }

    /**
     * Met à jour un avantage existant. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés, ce qui permet par ex. de le masquer via visible = false sans toucher au
     * reste.
     */
    public Benefit updateBenefit(
            long id, String icon, String label, Integer displayOrder, Boolean visible) {
        if (icon != null) {
            validateIcon(icon);
        }
        if (label != null) {
            validateLabel(label);
        }
        return benefitRepository
                .update(id, icon, label, displayOrder, visible)
                .orElseThrow(
                        () ->
                                new BenefitException.BenefitNotFoundException(
                                        "Avantage introuvable (id=" + id + ")"));
    }

    /**
     * Réordonne les avantages : orderedIds donne, du premier au dernier, l'ordre d'affichage
     * souhaité. Chaque id doit correspondre à un avantage existant.
     */
    public void reorderBenefits(List<Long> orderedIds) {
        Set<Long> uniqueIds = new HashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new BenefitException.InvalidBenefitRequestException(
                    "La liste des avantages à réordonner contient des doublons");
        }
        for (Long id : orderedIds) {
            benefitRepository
                    .findById(id)
                    .orElseThrow(
                            () ->
                                    new BenefitException.BenefitNotFoundException(
                                            "Avantage introuvable (id=" + id + ")"));
        }
        benefitRepository.updateDisplayOrders(orderedIds);
    }

    private void validateIcon(String icon) {
        if (icon.isBlank()) {
            throw new BenefitException.InvalidBenefitRequestException(
                    "L'icône ne peut pas être vide");
        }
        if (icon.length() > MAX_ICON_LENGTH) {
            throw new BenefitException.InvalidBenefitRequestException(
                    "L'icône dépasse la longueur maximale autorisée (" + MAX_ICON_LENGTH + ")");
        }
    }

    private void validateLabel(String label) {
        if (label.isBlank()) {
            throw new BenefitException.InvalidBenefitRequestException(
                    "Le libellé ne peut pas être vide");
        }
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new BenefitException.InvalidBenefitRequestException(
                    "Le libellé dépasse la longueur maximale autorisée (" + MAX_LABEL_LENGTH + ")");
        }
    }
}
