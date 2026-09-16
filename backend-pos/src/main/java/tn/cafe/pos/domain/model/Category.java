package tn.cafe.pos.domain.model;

import java.time.Instant;
import java.util.Objects;

public class Category {
    private String id;
    private String nom;
    private String description;
    private int ordre;
    private boolean active;
    private Instant creeLe;

    public Category(String id, String nom, String description, int ordre, boolean active, Instant creeLe) {
        setNom(nom);
        this.id = id;
        this.description = description;
        this.ordre = Math.max(0, ordre);
        this.active = active;
        this.creeLe = creeLe != null ? creeLe : Instant.now();
    }

    public static Category creer(String nom, String description, int ordre) {
        return new Category(null, nom, description, ordre, true, Instant.now());
    }

    public void renommer(String nom) { setNom(nom); }
    public void setNom(String nom) {
        if (nom == null || nom.isBlank()) throw new IllegalArgumentException("nom catégorie requis");
        if (nom.strip().length() < 2) throw new IllegalArgumentException("nom catégorie trop court");
        this.nom = nom.strip();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNom() { return nom; }
    public String getDescription() { return description; }
    public void setDescription(String d) { this.description = d; }
    public int getOrdre() { return ordre; }
    public void setOrdre(int o) { this.ordre = Math.max(0, o); }
    public boolean isActive() { return active; }
    public void setActive(boolean a) { this.active = a; }
    public Instant getCreeLe() { return creeLe; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
