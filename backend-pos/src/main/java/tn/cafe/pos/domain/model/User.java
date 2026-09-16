package tn.cafe.pos.domain.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Utilisateur gérant. Mot de passe stocké haché (hors domaine).
 * qrKey = clé personnelle scannée pour login. pin = code 4 chiffres.
 */
public class User {
    private String id;
    private String username;
    private String passwordHash;
    private String pinHash;
    private String qrKey;
    private Role role;
    private boolean actif;
    private Instant creeLe;

    public User(String id, String username, String passwordHash, String pinHash,
                String qrKey, Role role, boolean actif, Instant creeLe) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("username requis");
        if (role == null) throw new IllegalArgumentException("role requis");
        this.id = id;
        this.username = username.strip().toLowerCase();
        this.passwordHash = passwordHash;
        this.pinHash = pinHash;
        this.qrKey = qrKey;
        this.role = role;
        this.actif = actif;
        this.creeLe = creeLe != null ? creeLe : Instant.now();
    }

    public static User nouveauGerant(String username, String passwordHash, String pinHash, String qrKey) {
        return new User(null, username, passwordHash, pinHash, qrKey, Role.GERANT, true, Instant.now());
    }

    public void desactiver() { this.actif = false; }
    public void activer() { this.actif = true; }
    public void regenererQrKey(String nouvelleCle) {
        if (nouvelleCle == null || nouvelleCle.isBlank()) throw new IllegalArgumentException("qrKey invalide");
        this.qrKey = nouvelleCle;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public String getQrKey() { return qrKey; }
    public Role getRole() { return role; }
    public boolean isActif() { return actif; }
    public Instant getCreeLe() { return creeLe; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User u)) return false;
        return Objects.equals(id, u.id) && Objects.equals(username, u.username);
    }
    @Override public int hashCode() { return Objects.hash(id, username); }
}
