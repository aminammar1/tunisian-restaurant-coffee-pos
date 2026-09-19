package tn.cafe.pos.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
/** Mirrors the backend OrderItem JSON contract: nomProduit / prixUnitaire. */
public record OrderItem(String produitId, String nomProduit, BigDecimal prixUnitaire, int quantite) {
    /** Null-safe line total: a malformed payload must never crash the dashboard. */
    public BigDecimal sousTotal() {
        if (prixUnitaire == null) return BigDecimal.ZERO;
        return prixUnitaire.multiply(BigDecimal.valueOf(quantite));
    }
}
