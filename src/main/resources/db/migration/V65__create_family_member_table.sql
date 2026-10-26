-- Membres de la famille d'un utilisateur (parcours « للعائلة » de l'offre, voir
-- FamilyMemberService) : NNI et photo de la pièce d'identité de chaque membre, vérifiés par un
-- admin comme ceux de l'utilisateur (users.nni / id_photo_url / identity_verified). Un membre n'a
-- pas de compte : il est rattaché au compte qui l'a saisi.
CREATE TABLE family_member (
    id                 BIGSERIAL PRIMARY KEY,
    user_id            UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    nni                VARCHAR(50) NOT NULL,
    id_photo_url       VARCHAR(255) NOT NULL,
    identity_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL DEFAULT now()
);

-- Une personne ne participe qu'une fois à la tombola : NNI unique parmi les membres (l'unicité
-- avec users.nni est vérifiée par FamilyMemberService/UserService, pas de contrainte possible
-- entre deux tables).
CREATE UNIQUE INDEX uq_family_member_nni ON family_member (nni);

CREATE INDEX idx_family_member_user_id ON family_member (user_id);

-- Liste unique des participants à la tombola : NNI des utilisateurs et des membres de famille,
-- avec leur statut de vérification. Le tirage lira cette vue plutôt que les deux tables.
CREATE VIEW draw_participant AS
SELECT 'USER' AS kind,
       u.id::text AS participant_id,
       u.id AS owner_user_id,
       u.nni,
       u.identity_verified
FROM users u
WHERE u.nni IS NOT NULL
UNION ALL
SELECT 'FAMILY_MEMBER' AS kind,
       fm.id::text AS participant_id,
       fm.user_id AS owner_user_id,
       fm.nni,
       fm.identity_verified
FROM family_member fm;
