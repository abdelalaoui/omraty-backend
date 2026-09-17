-- Catalogue de voyages Omra (packages avec prix, destination, catégorie, images), affiché sur
-- CatalogScreen côté app (voir PackageModel). Concept distinct de package/OmraPackage (regroupement
-- de pèlerins pour les réservations de chambres, voir AdminPackageController) : nommée trip_package
-- pour ne pas entrer en collision, c'est une fiche produit/catalogue publique, pas un lot de
-- réservation.
CREATE TABLE trip_package (
    id             BIGSERIAL PRIMARY KEY,
    title          VARCHAR(255) NOT NULL,
    destination    VARCHAR(255) NOT NULL,
    category       VARCHAR(20) NOT NULL
        CHECK (category IN ('TOURISME', 'OMRA', 'PELERINAGE', 'AFFAIRES')),
    price          NUMERIC(10, 2) NOT NULL CHECK (price > 0),
    start_date     DATE NOT NULL,
    end_date       DATE NOT NULL,
    description    TEXT,
    includes_visa  BOOLEAN,
    group_size     INT,
    visible        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_trip_package_visible ON trip_package (visible);

-- Un package peut avoir plusieurs images (voir PackageModel.imageUrls) ; display_order fixe l'ordre
-- d'affichage côté app (galerie photo).
CREATE TABLE trip_package_image (
    id             BIGSERIAL PRIMARY KEY,
    package_id     BIGINT NOT NULL REFERENCES trip_package (id) ON DELETE CASCADE,
    url            VARCHAR(500) NOT NULL,
    display_order  INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_trip_package_image_package ON trip_package_image (package_id, display_order);
