-- Offre spéciale "réserver un lit en chambre de 5" à un prix différent du catalogue (voir
-- RoomService.reserveOfferBed, BookingPaymentService.resolveOfferPrice/getActiveOfferPrice) :
-- bed_offer_enabled contrôle si l'offre est actuellement active (et donc si la bannière l'expose,
-- voir BannerController), bed_offer_price son prix. Toujours en paiement complet (pas de tranches),
-- voir RoomService.reserveOfferBed. Désactivée par défaut tant que l'admin n'a pas choisi un prix.
INSERT INTO app_setting (key, value) VALUES ('bed_offer_enabled', 'false');
INSERT INTO app_setting (key, value) VALUES ('bed_offer_price', '0');
