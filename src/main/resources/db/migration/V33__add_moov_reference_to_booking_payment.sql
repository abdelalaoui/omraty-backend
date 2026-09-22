-- Une fois le paiement créé côté Moov (voir intégration du client HTTP à venir), le backend reçoit
-- un code de paiement et un identifiant de transaction à conserver : pour les renvoyer à l'app (le
-- code à afficher, sa date d'expiration), et pour retrouver l'achat concerné quand la confirmation
-- arrive (webhook ou vérification de statut). Colonnes nullable : une ligne créée avant
-- l'intégration Moov, ou en mode dégradé, peut ne pas les avoir.
ALTER TABLE booking_payment
    ADD COLUMN moov_payment_code VARCHAR(50),
    ADD COLUMN moov_transaction_id VARCHAR(100),
    ADD COLUMN payer_phone VARCHAR(20),
    ADD COLUMN expires_at TIMESTAMP;

-- moov_transaction_id est la clé qui garantit l'idempotence du webhook : un même événement Moov
-- rejoué plusieurs fois ne doit jamais correspondre à deux lignes booking_payment différentes.
CREATE UNIQUE INDEX uq_booking_payment_moov_transaction_id
    ON booking_payment (moov_transaction_id) WHERE moov_transaction_id IS NOT NULL;
