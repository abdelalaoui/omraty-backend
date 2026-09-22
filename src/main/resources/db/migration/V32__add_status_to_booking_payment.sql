-- Jusqu'ici booking_payment n'avait aucune notion de statut : un achat était considéré payé dès sa
-- création (voir BookingPaymentService.createInstallments, qui marquait la 1ère tranche payée
-- immédiatement). Impossible donc de savoir côté base si un paiement a réellement été effectué —
-- prérequis pour brancher une vraie passerelle de paiement (Moov).
--
-- Les achats déjà existants sont passés à CONFIRMED pour ne pas casser l'historique (ils ont déjà
-- été traités comme payés jusqu'ici) ; les nouveaux paiements démarrent PENDING (voir
-- BookingPaymentService.createPaymentPlan).
ALTER TABLE booking_payment
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED', 'EXPIRED'));

UPDATE booking_payment SET status = 'CONFIRMED';
