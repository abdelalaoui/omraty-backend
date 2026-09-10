-- Regroupement Omra (lot de pèlerins) auquel sont rattachées les chambres réservées/achetées
-- (voir room.package_id). group_size est le plafond global de places réservables sur toutes les
-- chambres du package, tous types confondus.
CREATE TABLE package (
    id          BIGSERIAL PRIMARY KEY,
    group_size  INT NOT NULL CHECK (group_size > 0)
);
