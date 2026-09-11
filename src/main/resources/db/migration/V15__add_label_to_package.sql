-- Libellé de la période de départ affiché à l'utilisateur (ex : "Omra Ramadan du 10 au 20 mars"),
-- utilisé par GET /reservation-groups pour choisir sa période avant de réserver une chambre (voir
-- ReservationGroupController). V15 plutôt que V14 : feature/service-cards-image-url (pas encore
-- mergée) a déjà réservé V14 sur main.
ALTER TABLE package ADD COLUMN label VARCHAR(255) NOT NULL;
