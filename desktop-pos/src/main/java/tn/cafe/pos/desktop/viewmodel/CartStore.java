package tn.cafe.pos.desktop.viewmodel;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import tn.cafe.pos.desktop.model.Product;

/** Shared cart: productId -> line. Single instance per app run. */
public class CartStore {
    private static final CartStore INSTANCE = new CartStore();
    public static CartStore get() { return INSTANCE; }

    public record Line(Product product, int qty) {
        public BigDecimal total() { return product.prix().multiply(BigDecimal.valueOf(qty)); }
    }

    private final ObservableList<Line> lines = FXCollections.observableArrayList();
    private final StringProperty tableOuClient = new SimpleStringProperty("Table 1");

    public ObservableList<Line> lines() { return lines; }
    public StringProperty tableOuClientProperty() { return tableOuClient; }

    public void add(Product p) {
        setQty(p, qtyOf(p.id()) + 1);
    }
    public void dec(Product p) {
        setQty(p, qtyOf(p.id()) - 1);
    }
    /** Fixed quantity from the customer stepper: 0 removes the line. */
    public void setQty(Product p, int qty) {
        if (p == null || p.id() == null) return;
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            if (l.product().id().equals(p.id())) {
                if (qty <= 0) lines.remove(i);
                else lines.set(i, new Line(p, qty));
                return;
            }
        }
        if (qty > 0) lines.add(new Line(p, qty));
    }
    public void remove(Product p) {
        if (p != null) setQty(p, 0);
    }
    /** Current quantity of a product in the basket, 0 when absent. */
    public int qtyOf(String productId) {
        if (productId == null) return 0;
        for (Line l : lines) {
            if (l.product().id() != null && l.product().id().equals(productId)) return l.qty();
        }
        return 0;
    }
    public void clear() { lines.clear(); }
    public boolean isEmpty() { return lines.isEmpty(); }
    public BigDecimal total() {
        return lines.stream().map(Line::total).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    /** Backend payload: [{produitId, quantite}]. */
    public List<Map<String, Object>> toOrderItems() {
        return lines.stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("produitId", l.product().id());
            m.put("quantite", l.qty());
            return m;
        }).toList();
    }
}
