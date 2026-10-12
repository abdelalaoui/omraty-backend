package com.omraty.backend.exception;

public class PromoPackageException extends RuntimeException {

    PromoPackageException(String message) {
        super(message);
    }

    /** Réservation de l'offre sur un package qui n'est pas (ou plus) dans la promo. → 409 */
    public static class PackageNotInOfferException extends PromoPackageException {
        public PackageNotInOfferException(String message) {
            super(message);
        }
    }

    /** Réservation par le parcours Omra normal d'un package réservé à l'offre. → 409 */
    public static class PromoPackageNotBookableException extends PromoPackageException {
        public PromoPackageNotBookableException(String message) {
            super(message);
        }
    }

    /** Package qui n'est pas dans la promo (côté admin). → 404 */
    public static class PromoPackageNotFoundException extends PromoPackageException {
        public PromoPackageNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidPromoPackageRequestException extends PromoPackageException {
        public InvalidPromoPackageRequestException(String message) {
            super(message);
        }
    }
}
