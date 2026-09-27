-- Réduction appliquée au paiement complet (plan FULL, voir BookingPaymentService.createPaymentPlan)
-- : jusqu'ici affichée côté app (payment_plan_screen.dart, 5% en dur) mais jamais réellement
-- appliquée par le backend — le client voyait un montant réduit à l'écran mais payait le montant
-- plein. 5 : valeur de départ alignée sur ce que l'app affichait déjà, modifiable ensuite sans
-- redéploiement via PATCH /admin/settings/full_payment_discount_percentage (déjà existant,
-- AdminSettingController). 0 = aucune réduction.
INSERT INTO app_setting (key, value) VALUES ('full_payment_discount_percentage', '5');
