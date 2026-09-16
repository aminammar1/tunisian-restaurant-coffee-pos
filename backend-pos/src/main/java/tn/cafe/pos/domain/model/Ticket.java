package tn.cafe.pos.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Ticket généré automatiquement après paiement : SERVICE (cuisine) + CLIENT (reçu). */
public class Ticket {
    private String id;
    private String commandeId;
    private String numeroCommande;
    private TicketType type;
    private String contenu;
    private Instant creeLe;

    public Ticket(String id, String commandeId, String numeroCommande, TicketType type, String contenu, Instant creeLe) {
        if (commandeId == null || commandeId.isBlank()) throw new IllegalArgumentException("commandeId requis");
        if (type == null) throw new IllegalArgumentException("type requis");
        if (contenu == null || contenu.isBlank()) throw new IllegalArgumentException("contenu requis");
        this.id = id;
        this.commandeId = commandeId;
        this.numeroCommande = numeroCommande;
        this.type = type;
        this.contenu = contenu;
        this.creeLe = creeLe != null ? creeLe : Instant.now();
    }

    public static Ticket creer(String commandeId, String numero, TicketType type, String contenu) {
        return new Ticket(null, commandeId, numero, type, contenu, Instant.now());
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommandeId() { return commandeId; }
    public String getNumeroCommande() { return numeroCommande; }
    public TicketType getType() { return type; }
    public String getContenu() { return contenu; }
    public Instant getCreeLe() { return creeLe; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket t)) return false;
        return Objects.equals(id, t.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
