-- Vérification manuelle admin d'un paiement (Moov, Bankily, Masrvi, Sedad... voir migration V50)
-- : confirmed_by_admin_id/confirmed_at tracent qui a confirmé et quand (voir
-- BookingPaymentService.confirmManually), même logique que booking_installment.paid_by_admin_id
-- (migration V38). rejection_reason conserve le motif du dernier rejet (voir
-- BookingPaymentService.rejectManually), pour garder une trace même si le paiement redevient
-- PENDING et peut être rejeté/confirmé plusieurs fois.
ALTER TABLE booking_payment
    ADD COLUMN confirmed_by_admin_id UUID,
    ADD COLUMN confirmed_at TIMESTAMP,
    ADD COLUMN rejection_reason TEXT;
