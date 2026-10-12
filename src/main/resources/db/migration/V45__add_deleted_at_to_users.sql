-- Suppression de compte (DELETE /users/me, voir UserService.deleteAccount) : le compte est
-- anonymisé en place plutôt que supprimé physiquement, car reservations/paiements référencent
-- users.id en FK et doivent être conservés (comptabilité/légal). deleted_at marque le compte
-- comme supprimé pour bloquer la connexion (voir AuthService.login/loginWithOtp).
ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP NULL;

-- phone est actuellement VARCHAR(20) UNIQUE : trop court pour y stocker "deleted-" + un UUID
-- (voir AuthRepository.anonymize), donc élargi ici.
ALTER TABLE users ALTER COLUMN phone TYPE VARCHAR(64);
