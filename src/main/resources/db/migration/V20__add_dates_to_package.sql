-- Vraies dates de départ/retour du package, pour permettre au frontend de calculer les échéances
-- de paiement en plusieurs tranches (tranche 2/3 basées sur la date de départ) : label restait
-- jusqu'ici un texte libre (ex. "Omra Ramadan du 10 au 20 mars"), sans date exploitable.
-- V20 plutôt que V17 : feature/notifications-push-fcm (pas encore mergée) a déjà réservé V17 sur
-- main (V17__create_notification_and_device_token_tables.sql), et V18/V19 sont déjà pris — cf. le
-- même choix pour V15 vs V14 et V18 vs V17.
-- Nullable : les lignes existantes (ex. "Omra Ramadan du 10 au 20 mars") n'ont pas de vraie date
-- connue, on ne devine rien à partir du label — l'admin les renseignera après coup via
-- PATCH /admin/packages/{id}. Tout nouveau package créé désormais les exige (voir
-- CreatePackageRequest).
ALTER TABLE package ADD COLUMN start_date DATE;
ALTER TABLE package ADD COLUMN end_date DATE;
