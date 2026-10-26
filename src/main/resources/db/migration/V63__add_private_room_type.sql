-- Chambre privée d'1 place (type 1) pour l'offre de la bannière BED_OFFER : option « لنفسي → غرفة
-- خاصة » de l'app, pour un client qui veut être seul, sans compagnon (voir
-- RoomService.reserveOfferPrivateRoom). Achetée entière comme une chambre de 2/3 (reserved_count =
-- total_capacity = 1, aucun lit), au prix « chambre privée » saisi par l'admin pour chaque package
-- promo (package_promo_price, type 1). Les contraintes CHECK de V10/V57 n'acceptaient que 2, 3, 5.
ALTER TABLE room DROP CONSTRAINT room_type_check;
ALTER TABLE room ADD CONSTRAINT room_type_check CHECK (type IN (1, 2, 3, 5));

ALTER TABLE package_promo_price DROP CONSTRAINT package_promo_price_type_check;
ALTER TABLE package_promo_price
    ADD CONSTRAINT package_promo_price_type_check CHECK (type IN (1, 2, 3, 5));
