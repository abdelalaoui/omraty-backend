-- Prix en lettres (arabe) d'un package promo, saisi par l'admin et affiché dans l'app sous le prix
-- en chiffres (voir PromoPackageService) — NULL = rien d'affiché.
ALTER TABLE package_promo ADD COLUMN price_text TEXT;
