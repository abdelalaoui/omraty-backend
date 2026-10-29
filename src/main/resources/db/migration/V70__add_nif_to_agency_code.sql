-- NIF de l'agence, saisi par l'admin avec le nom et le téléphone du compte client à qui le code
-- est destiné (voir AgencyCodeService.createAgencyCode). NULL pour les codes créés avant.
ALTER TABLE agency_code ADD COLUMN nif VARCHAR(30);
