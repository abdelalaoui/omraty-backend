package com.omraty.backend.controller;

import com.omraty.backend.dto.response.AdminPurchaseResponse;
import com.omraty.backend.mapper.PurchaseMapper;
import com.omraty.backend.service.AdminPurchase;
import com.omraty.backend.service.CustomOfferService;
import com.omraty.backend.service.RoomService;
import com.omraty.backend.storage.FileStorageService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation admin de toutes les réservations en cours (chambres achetées et lits réservés), tous
 * utilisateurs confondus, les plus récentes d'abord ; les places payées ensemble (famille, autres
 * personnes) forment une seule réservation avec ses personnes (voir RoomService.getAllPurchases),
 * et les propositions de l'agence y figurent dès que le client en a lancé le paiement (voir
 * CustomOfferService.listAdminBookings). Réservé à ROLE_ADMIN (voir SecurityConfig, préfixe
 * /admin/**).
 */
@RestController
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final RoomService roomService;
    private final CustomOfferService customOfferService;
    private final FileStorageService fileStorageService;

    public AdminBookingController(
            RoomService roomService,
            CustomOfferService customOfferService,
            FileStorageService fileStorageService) {
        this.roomService = roomService;
        this.customOfferService = customOfferService;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public ResponseEntity<List<AdminPurchaseResponse>> listBookings() {
        List<AdminPurchase> bookings =
                Stream.concat(
                                roomService.getAllPurchases().stream(),
                                customOfferService.listAdminBookings().stream())
                        .sorted(
                                Comparator.comparing(
                                                (AdminPurchase booking) ->
                                                        booking.purchase().createdAt())
                                        .reversed())
                        .toList();
        return ResponseEntity.ok(
                PurchaseMapper.toAdminResponseList(
                        bookings, fileStorageService::generatePresignedUrl));
    }
}
