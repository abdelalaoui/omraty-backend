package com.omraty.backend.service;

import com.omraty.backend.entities.PromoBed;
import com.omraty.backend.entities.PromoRoom;
import java.util.List;

/** Une chambre promo de type 5 avec ses lits, pour l'état des lits d'un package promo (GET). */
public record PromoRoomWithBeds(PromoRoom room, List<PromoBed> beds) {}
