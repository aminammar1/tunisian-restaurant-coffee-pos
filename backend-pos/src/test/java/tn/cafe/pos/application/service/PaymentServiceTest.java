package tn.cafe.pos.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.application.dto.PaymentRequest;
import tn.cafe.pos.application.dto.ProximityRequest;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.domain.repository.OrderRepository;
import tn.cafe.pos.domain.repository.PaymentRepository;
import tn.cafe.pos.infrastructure.realtime.OrderSseHub;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock OrderRepository commandes;
    @Mock PaymentRepository paiements;
    @Mock TicketService tickets;
    @Mock OrderSseHub hub;
    PaymentService service;

    @BeforeEach void setUp() { service = new PaymentService(commandes, paiements, tickets, hub); }

    private Order commande() {
        Order o = Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.500"), 2)), "T1");
        o.setId("o1"); o.setNumero("CMD-000001");
        return o;
    }

    @Test
    void confirmer_qr_genere_2_tickets() {
        Order o = commande();
        when(commandes.findById("o1")).thenReturn(Optional.of(o));
        when(paiements.save(any())).thenAnswer(i -> i.getArgument(0));
        when(commandes.save(any())).thenAnswer(i -> i.getArgument(0));
        Ticket s = Ticket.creer("o1", "CMD-000001", TicketType.SERVICE, "cuisine");
        Ticket c = Ticket.creer("o1", "CMD-000001", TicketType.CLIENT, "reçu");
        when(tickets.genererDeuxTickets(any())).thenReturn(List.of(s, c));

        var r = service.confirmer(new PaymentRequest("o1", PaymentType.QR));
        assertTrue(r.paiement().estReussi());
        assertEquals(OrderStatus.PAYEE, r.commande().getStatut());
        assertEquals(2, r.tickets().size());
        verify(hub).diffuser(any());
    }

    @Test
    void confirmer_deja_payee_refuse() {
        Order o = commande(); o.marquerPayee();
        when(commandes.findById("o1")).thenReturn(Optional.of(o));
        assertThrows(BusinessException.class, () -> service.confirmer(new PaymentRequest("o1", PaymentType.CASH)));
    }

    @Test
    void proximite_sans_signal_refuse() {
        assertThrows(BusinessException.class, () ->
                service.confirmerProximite(new ProximityRequest("o1", false)));
    }

    @Test
    void proximite_avec_signal_ok() {
        Order o = commande();
        when(commandes.findById("o1")).thenReturn(Optional.of(o));
        when(paiements.save(any())).thenAnswer(i -> i.getArgument(0));
        when(commandes.save(any())).thenAnswer(i -> i.getArgument(0));
        when(tickets.genererDeuxTickets(any())).thenReturn(List.of(
                Ticket.creer("o1", "CMD-1", TicketType.SERVICE, "s"),
                Ticket.creer("o1", "CMD-1", TicketType.CLIENT, "c")));
        var r = service.confirmerProximite(new ProximityRequest("o1", true));
        assertEquals(PaymentType.INFRARED, r.paiement().getType());
    }
}
