package tn.cafe.pos.infrastructure.printing;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Service;
import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.TicketType;
import tn.cafe.pos.domain.model.PaymentType;
import tn.cafe.pos.infrastructure.config.AppProperties;

/**
 * Rendu texte des tickets 58mm. Simulation d'impression locale.
 * Un seul responsable : mise en forme (SRP).
 */
@Service
public class TicketRenderer {
    private static final String RULE = "----------------------------";
    private static final String HEADER_RULE = "============================";
    private final AppProperties props;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public TicketRenderer(AppProperties props) { this.props = props; }

    public String rendre(Order order, TicketType type) {
        return rendre(order, type, null);
    }

    public String rendre(Order order, TicketType type, PaymentType paymentType) {
        String devise = props.ticket().currency();
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_RULE).append('\n');
        if (type == TicketType.SERVICE) {
            sb.append("   TICKET SERVICE / CUISINE\n");
        } else {
            sb.append("   TICKET CLIENT - REÇU\n");
        }
        sb.append(HEADER_RULE).append('\n');
        sb.append("Commande : ").append(order.getNumero() != null ? order.getNumero() : order.getId()).append("\n");
        if (order.getTableOuClient() != null) sb.append("Table/Client : ").append(order.getTableOuClient()).append("\n");
        sb.append("Date : ").append(order.getCreeLe().atZone(ZoneId.systemDefault()).format(FMT)).append("\n");
        sb.append(RULE).append('\n');
        order.getItems().forEach(i -> sb.append(String.format("%dx %s - %s %s%n",
                i.getQuantite(), i.getNomProduit(), i.sousTotal(), devise)));
        sb.append(RULE).append('\n');
        BigDecimal total = order.getTotal();
        sb.append(String.format("TOTAL : %s %s%n", total, devise));
        if (type == TicketType.CLIENT) {
            PaymentType selected = paymentType == null ? order.getPaiementTicket() : paymentType;
            sb.append("Paiement : ").append(selected == null ? "SIMULATION" : selected.name())
                    .append("\n");
            sb.append(props.ticket().footer()).append("\n");
        } else {
            sb.append("À préparer immédiatement.\n");
        }
        sb.append(HEADER_RULE).append('\n');
        sb.append("Imprimante : ").append(props.printer().name()).append(type == TicketType.CLIENT ? " - client" : " - cuisine").append("\n");
        return sb.toString();
    }
}
