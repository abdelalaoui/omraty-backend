-- Traçabilité de la réconciliation manuelle admin (PATCH /admin/installments/{id}/mark-paid).
-- Depuis le flux Moov (tâches 01-11), cet endpoint n'est plus le chemin principal de confirmation
-- mais reste un filet de sécurité pour les cas exceptionnels (litige, paiement reçu autrement,
-- panne prolongée côté Moov) : paid_manually distingue une tranche confirmée par Moov (false,
-- valeur par défaut) d'une validée manuellement, et paid_by_admin_id conserve l'admin qui l'a
-- déclenchée.
ALTER TABLE booking_installment
    ADD COLUMN paid_by_admin_id UUID REFERENCES users (id),
    ADD COLUMN paid_manually BOOLEAN NOT NULL DEFAULT false;
