package com.omraty.backend.service;

import com.omraty.backend.entities.ServiceCard;
import com.omraty.backend.exception.ServiceCardException;
import com.omraty.backend.repository.ServiceCardRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ServiceCardService {

    private static final int MAX_TYPE_LENGTH = 50;
    private static final int MAX_TITLE_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_BUTTON_TEXT_LENGTH = 100;
    private static final int MAX_ICON_LENGTH = 100;

    private final ServiceCardRepository serviceCardRepository;

    public ServiceCardService(ServiceCardRepository serviceCardRepository) {
        this.serviceCardRepository = serviceCardRepository;
    }

    /** Cartes actives à afficher sur la home, avec leur contenu et leur état (comingSoon). */
    public List<ServiceCard> getActiveServiceCards() {
        return serviceCardRepository.findActiveServiceCards();
    }

    /**
     * Ajoute une nouvelle carte de service. comingSoon/visible non fournis = valeurs par défaut
     * (pas en mode coming soon, visible) — extensible à un nouveau type de service sans changement
     * de code.
     */
    public ServiceCard createServiceCard(
            String type,
            String title,
            String description,
            String buttonText,
            String icon,
            Boolean comingSoon,
            Boolean visible) {
        validateType(type);
        validateTitle(title);
        validateDescription(description);
        validateButtonText(buttonText);
        validateIcon(icon);
        boolean isComingSoon = comingSoon != null && comingSoon;
        boolean isVisible = visible == null || visible;
        return serviceCardRepository.insert(
                type, title, description, buttonText, icon, isComingSoon, isVisible);
    }

    /**
     * Met à jour une carte existante. Tous les champs sont optionnels : seuls ceux fournis (non
     * null) sont modifiés — ex. basculer comingSoon (mode désactivé/popup) sans toucher au reste du
     * contenu.
     */
    public ServiceCard updateServiceCard(
            long id,
            String type,
            String title,
            String description,
            String buttonText,
            String icon,
            Boolean comingSoon,
            Boolean visible) {
        if (type != null) {
            validateType(type);
        }
        if (title != null) {
            validateTitle(title);
        }
        if (description != null) {
            validateDescription(description);
        }
        if (buttonText != null) {
            validateButtonText(buttonText);
        }
        if (icon != null) {
            validateIcon(icon);
        }
        return serviceCardRepository
                .update(id, type, title, description, buttonText, icon, comingSoon, visible)
                .orElseThrow(
                        () ->
                                new ServiceCardException.ServiceCardNotFoundException(
                                        "Carte de service introuvable (id=" + id + ")"));
    }

    private void validateType(String type) {
        if (type.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le type ne peut pas être vide");
        }
        if (type.length() > MAX_TYPE_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le type dépasse la longueur maximale autorisée (" + MAX_TYPE_LENGTH + ")");
        }
    }

    private void validateTitle(String title) {
        if (title.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le titre ne peut pas être vide");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le titre dépasse la longueur maximale autorisée (" + MAX_TITLE_LENGTH + ")");
        }
    }

    private void validateDescription(String description) {
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "La description dépasse la longueur maximale autorisée ("
                            + MAX_DESCRIPTION_LENGTH
                            + ")");
        }
    }

    private void validateButtonText(String buttonText) {
        if (buttonText.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le texte du bouton ne peut pas être vide");
        }
        if (buttonText.length() > MAX_BUTTON_TEXT_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "Le texte du bouton dépasse la longueur maximale autorisée ("
                            + MAX_BUTTON_TEXT_LENGTH
                            + ")");
        }
    }

    private void validateIcon(String icon) {
        if (icon.isBlank()) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'icône ne peut pas être vide");
        }
        if (icon.length() > MAX_ICON_LENGTH) {
            throw new ServiceCardException.InvalidServiceCardRequestException(
                    "L'icône dépasse la longueur maximale autorisée (" + MAX_ICON_LENGTH + ")");
        }
    }
}
