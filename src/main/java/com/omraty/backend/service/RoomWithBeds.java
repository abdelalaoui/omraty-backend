package com.omraty.backend.service;

import com.omraty.backend.entities.Bed;
import com.omraty.backend.entities.Room;
import java.util.List;

/** Une chambre de type 5 avec ses lits, pour l'état des lits d'un package (GET). */
public record RoomWithBeds(Room room, List<Bed> beds) {}
