-- Délai avant expiration du code de paiement d'un moyen de paiement manuel (voir
-- BookingPaymentService, PaymentExpirationService) : contrairement au flux Moov (15 min, délai
-- adapté à une confirmation automatique quasi immédiate), un virement/dépôt wallet manuel prend
-- plus de temps. 1440 min (24h) choisi par défaut en attendant un délai précis convenu avec
-- l'agence — modifiable sans redéploiement via PATCH /admin/settings/{key}.
INSERT INTO app_setting (key, value) VALUES ('manual_payment_expiration_minutes', '1440');
