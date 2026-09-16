package tn.cafe.pos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class Product {
    private String id;
    private String nom;
    private BigDecimal prix;
    private String categorieId;
    private String description;
    private String imageUrl;
    private boolean disponible;
    private Instant creeLe;

    public Product(String id, String nom, BigDecimal prix, String categorieId,
                   String description, String imageUrl, boolean disponible, Instant creeLe) {
        setNom(nom);
        setPrix(prix);
        setCategorieId(categorieId);
        this.id = id;
        this.description = description;
        this.imageUrl = imageUrl;
        this.disponible = disponible;
        this.creeLe = creeLe != null ? creeLe : Instant.now();
    }

    public static Product creer(String nom, BigDecimal prix, String categorieId,
                                String description, String imageUrl) {
        return new Product(null, nom, prix, categorieId, description, imageUrl, true, Instant.now());
    }

    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("nom produit requis");
        this.nom = nom.strip();
    }

    public void setPrix(BigDecimal prix) {
        if (prix == null || prix.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("prix TND invalide");
        this.prix = prix;
    }

    public void setCategorieId(String categorieId) {
        if (categorieId == null || categorieId.isBlank()) throw new IllegalArgumentException("categorieId requis");
        this.categorieId = categorieId;
    }

    public void marquerDisponible(boolean d) { this.disponible = d; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public BigDecimal getPrix() { return prix; }
    public String getCategorieId() { return categorieId; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String u) { this.imageUrl = u; }
    public boolean isDisponible() { return disponible; }
    public Instant getCreeLe() { return creeLe; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product p)) return false;
        return Objects.equals(id, p.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
