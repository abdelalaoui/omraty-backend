package com.omraty.backend.controller;

import com.omraty.backend.dto.request.CreateServiceCardRequest;
import com.omraty.backend.dto.request.UpdateServiceCardRequest;
import com.omraty.backend.dto.response.ServiceCardResponse;
import com.omraty.backend.entities.ServiceCard;
import com.omraty.backend.mapper.ServiceCardMapper;
import com.omraty.backend.service.ServiceCardService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cartes de services (Omra/Hajj/...) de la home : liste dynamique pilotée depuis le backend, pas
 * figée à 2 cartes — en ajouter une nouvelle (ex : un 3ème type de service) ou en modifier une (y
 * compris basculer comingSoon) ne nécessite pas de redéploiement de l'app. GET accessible à tout
 * utilisateur authentifié ; POST/PATCH réservés à ROLE_ADMIN (voir SecurityConfig).
 */
@RestController
@RequestMapping("/home/service-cards")
public class ServiceCardController {

    private final ServiceCardService serviceCardService;

    public ServiceCardController(ServiceCardService serviceCardService) {
        this.serviceCardService = serviceCardService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceCardResponse>> listActiveServiceCards() {
        return ResponseEntity.ok(
                ServiceCardMapper.toResponseList(serviceCardService.getActiveServiceCards()));
    }

    @PostMapping
    public ResponseEntity<ServiceCardResponse> createServiceCard(
            @Valid @RequestBody CreateServiceCardRequest request) {
        ServiceCard serviceCard =
                serviceCardService.createServiceCard(
                        request.type(),
                        request.title(),
                        request.description(),
                        request.buttonText(),
                        request.icon(),
                        request.comingSoon(),
                        request.visible());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ServiceCardMapper.toResponse(serviceCard));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ServiceCardResponse> updateServiceCard(
            @PathVariable long id, @RequestBody UpdateServiceCardRequest request) {
        ServiceCard serviceCard =
                serviceCardService.updateServiceCard(
                        id,
                        request.type(),
                        request.title(),
                        request.description(),
                        request.buttonText(),
                        request.icon(),
                        request.comingSoon(),
                        request.visible());
        return ResponseEntity.ok(ServiceCardMapper.toResponse(serviceCard));
    }
}
