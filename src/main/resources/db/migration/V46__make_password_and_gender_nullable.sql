-- Un compte peut désormais naître sans mot de passe ni genre (voir
-- AuthService.requestOtp, qui crée le compte à la volée pour un numéro
-- inconnu avant d'envoyer le code OTP) : le genre est alors collecté plus
-- tard, en même temps que le NNI/la photo (voir UserService.updateIdentity).
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ALTER COLUMN gender DROP NOT NULL;
