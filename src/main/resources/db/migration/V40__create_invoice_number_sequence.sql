-- Numérotation séquentielle des factures (voir InvoiceService.buildInvoiceNumber) : une SEQUENCE
-- PostgreSQL est atomique par construction, contrairement à un compteur calculé côté Java qui
-- pourrait attribuer le même numéro à deux factures générées en même temps. Le numéro final est
-- formaté "OMR-{année}-{nextval sur 6 chiffres}" ; la séquence elle-même ne se réinitialise jamais
-- (seule l'étiquette {année} change d'une facture à l'autre).
CREATE SEQUENCE invoice_number_seq START WITH 100000;
