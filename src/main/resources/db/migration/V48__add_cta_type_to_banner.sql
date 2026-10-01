-- Logique de bouton CTA configurable par bannière (voir BannerController, PromoBanner côté app) :
-- NULL = bouton visuel sans action réelle (comportement actuel). 'BED_OFFER' = ouvre le parcours de
-- réservation de lit à prix spécial (voir bed_offer_enabled/bed_offer_price, migration V47) ; seule
-- valeur reconnue pour l'instant côté BannerService, d'autres pourront s'ajouter pour de futures
-- offres sans nouvelle migration.
ALTER TABLE banner ADD COLUMN cta_type VARCHAR(20);
