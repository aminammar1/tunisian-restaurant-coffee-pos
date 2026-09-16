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
        return items.stream().map(OrderItem::sousTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
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
        this.statut = next;
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

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order x)) return false;
        return Objects.equals(id, x.id);
    }
    @Override public int hashCode() { return Objects.hashCode(id); }
}
