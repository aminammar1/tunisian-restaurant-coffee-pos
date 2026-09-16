package tn.cafe.pos.application.service;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.domain.repository.TicketRepository;
import tn.cafe.pos.infrastructure.config.AppProperties;
import tn.cafe.pos.infrastructure.printing.TicketRenderer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {
    @Mock TicketRepository repo;

    private TicketRenderer renderer() {
        return new TicketRenderer(new AppProperties(null, null,
                new AppProperties.Printer(true, "POS-58mm"),
                new AppProperties.Ticket(58, "TND", "fr-TN", "Merci !"), null));
    }

    @Test
    void genererDeuxTickets_service_et_client() {
        TicketService service = new TicketService(repo, renderer());
        Order o = Order.creer(List.of(new OrderItem("p1", "Café", new BigDecimal("2.000"), 1)), "T2");
        o.setId("o9"); o.setNumero("CMD-000009");
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        List<Ticket> deux = service.genererDeuxTickets(o);
        assertEquals(2, deux.size());
        assertTrue(deux.stream().anyMatch(t -> t.getType() == TicketType.SERVICE));
        assertTrue(deux.stream().anyMatch(t -> t.getType() == TicketType.CLIENT));
        assertTrue(deux.get(1).getContenu().contains("TND"));
    }
}
