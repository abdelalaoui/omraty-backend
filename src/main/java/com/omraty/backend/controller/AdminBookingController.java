package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminPurchaseResponse;
import com.omraty.backend.mapper.PurchaseMapper;
import com.omraty.backend.service.RoomService;
import com.omraty.backend.storage.FileStorageService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation admin de toutes les réservations en cours (chambres achetées et lits réservés), tous
 * utilisateurs confondus, les plus récentes d'abord ; les places payées ensemble (famille, autres
 * personnes) forment une seule réservation avec ses personnes (voir RoomService.getAllPurchases).
 * Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe /admin/**).
 */
@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final RoomService roomService;
    private final FileStorageService fileStorageService;

    public AdminBookingController(RoomService roomService, FileStorageService fileStorageService) {
        this.roomService = roomService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<AdminPurchaseResponse>> listBookings() {
        return ResponseEntity.ok(
                PurchaseMapper.toAdminResponseList(
                        roomService.getAllPurchases(), fileStorageService::generatePresignedUrl));
    }
}
