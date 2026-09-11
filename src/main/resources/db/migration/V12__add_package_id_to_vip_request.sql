-- Rattache chaque demande VIP à un package (voir package.group_size). Les places des demandes VIP
-- actives comptent désormais dans le même plafond que les places réservées en chambre (voir
-- RoomRepository.sumVipSeatsForPackage et PackageCapacityService) : les deux systèmes partagent
-- un seul calcul de plafond, aucun ne peut plus dépasser group_size indépendamment de l'autre.
ALTER TABLE vip_request ADD COLUMN package_id BIGINT NOT NULL REFERENCES package (id);

-- Sert au calcul du plafond (somme des seats des demandes actives pour un package).
CREATE INDEX idx_vip_request_package_status ON vip_request (package_id, status);
