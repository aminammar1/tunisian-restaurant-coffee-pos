package tn.cafe.pos.domain.model;

/**
 * Types de paiement SIMULÉS. Aucun vrai gateway.
 * INFRARED = détection proximité téléphone/carte (simulée).
 */
public enum PaymentType {
    QR,
    APPLE_PAY,
    CARD,
    CASH,
    INFRARED
}
