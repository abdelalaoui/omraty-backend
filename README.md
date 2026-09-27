# Omraty Backend

Backend Spring Boot pour l'application OMRATY (réservation de voyages Omra : catalogue, chambres/lits,
offres VIP, paiement Moov, notifications).

Conventions reprises du backend sehdini (`auth-identity`), adaptées à un projet monolithe standalone :
JDBC (`JdbcTemplate`, pas de JPA), JWT access + refresh (refresh token persisté en base, révocable, avec
rotation), BCrypt, DTOs en `record`, Flyway pour les migrations.

## Structure

```
src/main/java/com/omraty/backend/
  config/       configuration Spring (sécurité JWT, Firebase, upload...)
  controller/   contrôleurs REST (publics, /admin/*, webhooks)
  dto/          DTOs (record) échangés via l'API (requêtes/réponses)
  entities/     entités métier (record) + enums (PaymentStatus, PaymentPlan, VipRequestStatus...)
  exception/    exceptions typées + error codes + handler global
  mapper/       conversion entités <-> DTOs
  payment/      passerelle de paiement (PaymentGatewayClient, WebhookVerifier + implémentations mock)
  push/         envoi de notifications push (FirebasePushSender)
  repository/   accès JDBC à la base (JdbcTemplate)
  scheduler/    tâches planifiées (expiration paiement/VIP, jobs de vérification, rappels...)
  service/      logique métier
```

## Lancer en local

Prérequis : Java 21, PostgreSQL.

```bash
createdb omraty
cp .env.example .env   # puis exporter ces variables, ou les passer directement à bootRun
./gradlew bootRun
```

Toutes les variables d'environnement sont documentées dans `.env.example` (valeurs par défaut de dev
déjà renseignées) : base de données, JWT, paiement Moov, VIP, notifications push (Firebase, optionnel),
stockage des fichiers (local par défaut, S3 en option). Un nouveau développeur peut démarrer uniquement
avec ce fichier.

## État actuel

Fonctionnalités backend construites et couvertes par des tests unitaires :

- [x] **Authentification** : inscription/connexion par téléphone, JWT access + refresh (rotation,
      révocation), vérification de code agence, complétion de profil (NNI + photo d'identité),
      validation admin de l'identité.
- [x] **Catalogue** : packages de voyage (formules Omra), hôtels Mecque/Médine, bannières promo,
      avantages (benefits), service cards — gestion dynamique côté admin (CRUD + i18n).
- [x] **Chambres et lits** : ouverture de chambre partagée, réservation de lit, achat de chambre
      complète, formules de service (`ServiceTier`, tarifs par capacité), suivi des achats/réservations
      de l'utilisateur (`GET /users/me/purchases`).
- [x] **VIP** : demandes VIP, proposition de prix par l'admin, acceptation/expiration automatique de
      l'offre, paiement d'une offre VIP acceptée.
- [x] **Paiement (Moov)** : plan de paiement complet ou en 3 tranches (60/20/20 %) à la réservation,
      intégration passerelle de paiement (`PaymentGatewayClient`, implémentation mock par défaut en
      dev/test), webhook de confirmation (`POST /webhooks/moov`) avec vérification d'authenticité
      (`WebhookVerifier`), statuts `PENDING`/`CONFIRMED`/`FAILED`/`EXPIRED`, confirmation idempotente,
      job de vérification de secours des paiements `PENDING` restés sans réponse
      (`PendingPaymentCheckTask`), job d'expiration automatique des paiements dépassés avec libération
      de la chambre/du lit (`PaymentExpirationTask`), rappels de tranches à venir, validation manuelle
      admin d'une tranche en cas de litige (avec trace d'audit).
- [x] **Notifications** : notifications in-app + push FCM (Firebase), enregistrement du token
      appareil, marquage comme lues.
- [x] **Divers** : vérification de version de l'app (`/app/version-check`), nettoyage automatique des
      refresh tokens expirés.

Détail de chaque tâche : voir le board Trello du projet.

## Tests

```bash
./gradlew test
```
