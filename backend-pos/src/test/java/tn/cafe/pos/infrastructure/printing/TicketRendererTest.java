package tn.cafe.pos.infrastructure.printing;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.infrastructure.config.AppProperties;

import static org.junit.jupiter.api.Assertions.*;

class TicketRendererTest {
    private TicketRenderer renderer() {
        return new TicketRenderer(new AppProperties(null, null,
                new AppProperties.Printer(true, "POS-58mm"),
                new AppProperties.Ticket(58, "TND", "fr-TN", "Merci de votre visite !"), null, null));
    }

    private Order order() {
        Order o = Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.500"), 2)), "T5");
        o.setId("o1"); o.setNumero("CMD-000001");
        return o;
    }

    @Test
    void ticket_client_contient_total_et_merci() {
        String txt = renderer().rendre(order(), TicketType.CLIENT);
        assertTrue(txt.contains("TICKET CLIENT"));
        assertTrue(txt.contains("5.000"));
        assertTrue(txt.contains("TND"));
        assertTrue(txt.contains("Merci"));
    }

    @Test
    void ticket_service_contient_cuisine() {
        String txt = renderer().rendre(order(), TicketType.SERVICE);
        assertTrue(txt.contains("CUISINE"));
    }
}
