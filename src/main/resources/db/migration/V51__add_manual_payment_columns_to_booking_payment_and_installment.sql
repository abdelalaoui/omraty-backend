-- Paiement manuel (voir migration V50, PaymentMethod) en plus du flux Moov existant : le client
-- choisit un moyen de paiement à la création (paramètre paymentMethod), reçoit un code de paiement
-- (payment_reference) à utiliser dans son wallet, puis déclare avoir payé (declared_at) — voir
-- BookingPaymentService. payment_method/payment_reference/declared_at restent NULL pour une
-- requête sans paymentMethod (compatibilité avec les app 1.0/1.0.1, voir BookingPaymentService) :
-- le flux Moov (moov_payment_code/moov_transaction_id) reste inchangé dans ce cas.
--
-- payment_reference représente toujours « le paiement dû maintenant » (montant complet en FULL,
-- 1ère tranche en INSTALLMENTS) — les tranches 2/3 ont leur propre code sur booking_installment,
-- jamais sur booking_payment pour ces cas-là (voir BookingPaymentService).
ALTER TABLE booking_payment
    ADD COLUMN payment_method VARCHAR(30) REFERENCES payment_method (code),
    ADD COLUMN payment_reference VARCHAR(20),
    ADD COLUMN declared_at TIMESTAMP;

CREATE UNIQUE INDEX uq_booking_payment_payment_reference
    ON booking_payment (payment_reference) WHERE payment_reference IS NOT NULL;

-- Nouveau statut PENDING_VERIFICATION (voir PaymentStatus) : paiement manuel déclaré par le
-- client, en attente de vérification par l'admin — n'expire plus (voir PaymentExpirationService,
-- qui ne cible que le statut PENDING).
ALTER TABLE booking_payment DROP CONSTRAINT booking_payment_status_check;
ALTER TABLE booking_payment ADD CONSTRAINT booking_payment_status_check
    CHECK (status IN ('PENDING', 'PENDING_VERIFICATION', 'CONFIRMED', 'FAILED', 'EXPIRED'));

-- Même mécanisme pour les tranches 2/3 d'un plan INSTALLMENTS payé via un moyen de paiement
-- manuel : chacune a son propre code (la 1ère est représentée par booking_payment.payment_reference
-- ci-dessus, jamais ici) — voir BookingPaymentService.
ALTER TABLE booking_installment
    ADD COLUMN payment_method VARCHAR(30) REFERENCES payment_method (code),
    ADD COLUMN payment_reference VARCHAR(20),
    ADD COLUMN declared_at TIMESTAMP;

CREATE UNIQUE INDEX uq_booking_installment_payment_reference
    ON booking_installment (payment_reference) WHERE payment_reference IS NOT NULL;
