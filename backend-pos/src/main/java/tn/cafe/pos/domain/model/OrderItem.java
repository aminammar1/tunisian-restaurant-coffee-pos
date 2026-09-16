package tn.cafe.pos.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

/** Ligne de commande : objet-valeur immuable. */
public class OrderItem {
    private final String produitId;
    private final String nomProduit;
    private final BigDecimal prixUnitaire;
    private final int quantite;

    public OrderItem(String produitId, String nomProduit, BigDecimal prixUnitaire, int quantite) {
        if (produitId == null || produitId.isBlank()) throw new IllegalArgumentException("produitId requis");
        if (prixUnitaire == null || prixUnitaire.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("prix unitaire invalide");
        if (quantite <= 0) throw new IllegalArgumentException("quantité >= 1 requise");
        this.produitId = produitId;
        this.nomProduit = nomProduit != null ? nomProduit : produitId;
        this.prixUnitaire = prixUnitaire;
        this.quantite = quantite;
    }

    public BigDecimal sousTotal() { return prixUnitaire.multiply(BigDecimal.valueOf(quantite)); }

    public String getProduitId() { return produitId; }
    public String getNomProduit() { return nomProduit; }
    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public int getQuantite() { return quantite; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem i)) return false;
        return Objects.equals(produitId, i.produitId) && quantite == i.quantite
                && Objects.equals(prixUnitaire, i.prixUnitaire);
    }
    @Override public int hashCode() { return Objects.hash(produitId, prixUnitaire, quantite); }
}
