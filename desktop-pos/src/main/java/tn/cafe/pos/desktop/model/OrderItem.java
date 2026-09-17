package tn.cafe.pos.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
/** Mirrors the backend OrderItem JSON contract: nomProduit / prixUnitaire. */
public record OrderItem(String produitId, String nomProduit, BigDecimal prixUnitaire, int quantite) {
    public BigDecimal sousTotal() { return prixUnitaire.multiply(BigDecimal.valueOf(quantite)); }
}
