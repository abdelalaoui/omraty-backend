-- Référence de la facture PDF générée une fois l'achat intégralement payé (voir InvoiceService).
-- Les 3 colonnes restent NULL tant qu'aucune facture n'a été générée ; invoice_generated_at sert de
-- garde d'idempotence (une seule génération par achat, voir
-- BookingPaymentRepository.markInvoiceGenerated).
ALTER TABLE booking_payment
    ADD COLUMN invoice_key VARCHAR(255),
    ADD COLUMN invoice_number VARCHAR(30),
    ADD COLUMN invoice_generated_at TIMESTAMP;
