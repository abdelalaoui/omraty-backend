-- Nom du client (propriétaire du compte à qui le code est destiné) et NIF de l'agence
-- (facultatif), saisis par l'admin avec le nom de l'agence et le téléphone du compte client (voir
-- AgencyCodeService.createAgencyCode). NULL pour les codes créés avant.
ALTER TABLE agency_code ADD COLUMN client_name VARCHAR(255);
ALTER TABLE agency_code ADD COLUMN nif VARCHAR(30);
