-- Image affichée dans le cercle de chaque carte (voir service_card.icon, jusqu'ici un simple nom
-- d'icône mappé à un asset local côté app). image_url est optionnelle : réglable directement (URL
-- déjà hébergée) via POST/PATCH /home/service-cards, ou par upload admin (voir
-- PATCH /home/service-cards/{id}/image, même mécanisme S3/local que la bannière).
ALTER TABLE service_card ADD COLUMN image_url VARCHAR(500);
