package com.omraty.backend.service;

import java.util.UUID;

/**
 * Une réservation vue par l'admin (GET /admin/bookings, voir RoomService.getAllPurchases) : la
 * réservation telle que la voit son propriétaire (voir UserPurchase), plus son identifiant
 * ("room-12" pour une chambre entière, "bed-34" pour un lit) et le téléphone du propriétaire.
 * userPhone est null si le compte n'existe plus.
 */
public record AdminPurchase(String id, UUID userId, String userPhone, UserPurchase purchase) {}
