package tn.cafe.pos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** Paiement 100% simulé : tout QR / proximité / carte / cash = REUSSI sauf montant invalide. */
public class Payment {
    private String id;
    private String commandeId;
    private PaymentType type;
    private BigDecimal montant;
    private PaymentStatus statut;
    private String referenceSimulee;
    private Instant creeLe;

    public Payment(String id, String commandeId, PaymentType type, BigDecimal montant,
                   PaymentStatus statut, String referenceSimulee, Instant creeLe) {
        if (commandeId == null || commandeId.isBlank()) throw new IllegalArgumentException("commandeId requis");
        if (type == null) throw new IllegalArgumentException("type requis");
        if (montant == null || montant.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("montant invalide");
        this.id = id;
        this.commandeId = commandeId;
        this.type = type;
        this.montant = montant;
        this.statut = statut != null ? statut : PaymentStatus.EN_ATTENTE;
        this.referenceSimulee = referenceSimulee;
        this.creeLe = creeLe != null ? creeLe : Instant.now();
    }

    public static Payment simulerSucces(String commandeId, PaymentType type, BigDecimal montant, String ref) {
        return new Payment(null, commandeId, type, montant, PaymentStatus.REUSSI, ref, Instant.now());
    }

    public void marquerReussi() { this.statut = PaymentStatus.REUSSI; }
    public void marquerEchoue() { this.statut = PaymentStatus.ECHOUE; }
    public boolean estReussi() { return statut == PaymentStatus.REUSSI; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCommandeId() { return commandeId; }
    public PaymentType getType() { return type; }
    public BigDecimal getMontant() { return montant; }
    public PaymentStatus getStatut() { return statut; }
    public String getReferenceSimulee() { return referenceSimulee; }
    public Instant getCreeLe() { return creeLe; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment p)) return false;
        return Objects.equals(id, p.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
