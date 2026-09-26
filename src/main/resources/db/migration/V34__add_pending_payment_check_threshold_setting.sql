-- Délai (en minutes) au-delà duquel un booking_payment PENDING est considéré assez vieux pour
-- justifier une vérification de secours auprès de la passerelle (voir PendingPaymentCheckService,
-- PendingPaymentCheckTask) : le webhook Moov (tâche 05) est le chemin normal de confirmation, ce
-- job ne fait qu'interroger nous-mêmes Moov (PaymentGatewayClient.checkStatus) pour les paiements
-- que le webhook n'aurait pas confirmés à temps (appel réseau échoué ou jamais reçu côté banque).
-- 5 minutes : largement au-dessus du délai de confirmation simulé par le mock (10s, voir
-- MockPaymentGatewayClient), pour ne pas dupliquer inutilement le travail du webhook sur un
-- paiement tout juste créé.
INSERT INTO app_setting (key, value) VALUES ('pending_payment_check_threshold_minutes', '5');
