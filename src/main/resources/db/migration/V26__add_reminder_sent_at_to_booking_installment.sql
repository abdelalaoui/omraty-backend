-- Marque qu'un rappel de paiement (notification push) a déjà été envoyé pour cette tranche, pour
-- que la tâche planifiée quotidienne (voir InstallmentReminderTask/PaymentReminderService) ne
-- notifie le client qu'une seule fois pour la 3e tranche, même si due_date est déjà dépassée depuis
-- plusieurs jours (rattrapage si la tâche a manqué une exécution) ou si le job tourne plusieurs fois
-- de suite.
ALTER TABLE booking_installment ADD COLUMN reminder_sent_at TIMESTAMP;
