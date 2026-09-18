package tn.cafe.pos.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Order {
    private String id;
    private String numero;
    private List<OrderItem> items;
    private BigDecimal total;
    private OrderStatus statut;
    private String tableOuClient;
    private Instant creeLe;
    private PaymentType paiementTicket;

    public Order(String id, String numero, List<OrderItem> items, BigDecimal total,
                 OrderStatus statut, String tableOuClient, Instant creeLe) {
        if (items == null || items.isEmpty()) throw new IllegalArgumentException("commande vide");
        this.id = id;
        this.numero = numero;
        this.items = new ArrayList<>(items);
        this.total = total != null ? total : calculerTotal(items);
        this.statut = statut != null ? statut : OrderStatus.EN_ATTENTE;
        this.tableOuClient = tableOuClient;
        this.creeLe = creeLe != null ? creeLe : Instant.now();
    }

    public static Order creer(List<OrderItem> items, String tableOuClient) {
        return new Order(null, null, items, calculerTotal(items), OrderStatus.EN_ATTENTE, tableOuClient, Instant.now());
    }

    public static BigDecimal calculerTotal(List<OrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) total = total.add(item.sousTotal());
        return total;
    }

    public void confirmer() {
        if (statut != OrderStatus.EN_ATTENTE) throw new IllegalStateException("seule EN_ATTENTE peut être confirmée");
        statut = OrderStatus.CONFIRMEE;
    }

    public void marquerPayee() {
        if (statut == OrderStatus.ANNULEE) throw new IllegalStateException("commande annulée");
        statut = OrderStatus.PAYEE;
    }

    public void annuler() {
        if (statut == OrderStatus.PAYEE) throw new IllegalStateException("commande payée non annulable");
        statut = OrderStatus.ANNULEE;
    }

    public void changerStatut(OrderStatus next) {
        if (next == null) throw new IllegalArgumentException("statut requis");
        if (next == OrderStatus.PAYEE) {
            throw new IllegalStateException("seul le paiement peut marquer une commande payée");
        }
        if (next == OrderStatus.ANNULEE) {
            throw new IllegalStateException("utilisez l'annulation pour annuler une commande");
        }
        if (statut == OrderStatus.PAYEE || statut == OrderStatus.ANNULEE) {
            throw new IllegalStateException("commande terminée : statut non modifiable");
        }
        if (!transitionAutorisee(statut, next)) {
            throw new IllegalStateException("transition de statut invalide : " + statut + " vers " + next);
        }
        this.statut = next;
    }

    private static boolean transitionAutorisee(OrderStatus current, OrderStatus next) {
        return switch (current) {
            case EN_ATTENTE -> next == OrderStatus.CONFIRMEE || next == OrderStatus.EN_PREPARATION;
            case CONFIRMEE -> next == OrderStatus.EN_PREPARATION;
            case EN_PREPARATION -> next == OrderStatus.PRETE;
            case PRETE, PAYEE, ANNULEE -> false;
        };
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getNumero() { return numero; }
    public void setNumero(String n) { this.numero = n; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
    public BigDecimal getTotal() { return total; }
    public OrderStatus getStatut() { return statut; }
    public String getTableOuClient() { return tableOuClient; }
    public Instant getCreeLe() { return creeLe; }
    public PaymentType getPaiementTicket() { return paiementTicket; }
    public void definirPaiementTicket(PaymentType type) { this.paiementTicket = type; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order x)) return false;
        return Objects.equals(id, x.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
