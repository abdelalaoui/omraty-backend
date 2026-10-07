package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminServiceCardResponse;
import com.omraty.backend.mapper.ServiceCardMapper;
import com.omraty.backend.service.ServiceCardService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lecture et suppression admin des cartes de services, cartes masquées comprises (voir {@link
 * ServiceCardController} pour la liste publique filtrée/résolue par langue, et la création/édition
 * déjà réservées à ROLE_ADMIN sous /home/service-cards). Réservé aux comptes ROLE_ADMIN (voir
 * SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/service-cards")
public class AdminServiceCardController {

    private final ServiceCardService serviceCardService;

    public AdminServiceCardController(ServiceCardService serviceCardService) {
        this.serviceCardService = serviceCardService;
    }

    @GetMapping
    public ResponseEntity<List<AdminServiceCardResponse>> listServiceCards() {
        return ResponseEntity.ok(
                ServiceCardMapper.toAdminResponseList(serviceCardService.getAllServiceCards()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceCard(@PathVariable long id) {
        serviceCardService.deleteServiceCard(id);
        return ResponseEntity.noContent().build();
    }
}
