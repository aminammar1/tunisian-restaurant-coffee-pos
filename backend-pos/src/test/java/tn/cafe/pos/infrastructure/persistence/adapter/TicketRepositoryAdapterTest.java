package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.Ticket;
import tn.cafe.pos.domain.model.TicketType;
import tn.cafe.pos.infrastructure.persistence.document.TicketDocument;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringTicketMongo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketRepositoryAdapterTest {
    @Mock SpringTicketMongo spring;
    @InjectMocks TicketRepositoryAdapter adapter;

    private TicketDocument doc(String type) {
        TicketDocument d = new TicketDocument();
        d.id = "t1"; d.commandeId = "o1"; d.numeroCommande = "CMD-1";
        d.type = type; d.contenu = "contenu";
        return d;
    }

    @Test
    void save_find() {
        when(spring.save(any())).thenReturn(doc("CLIENT"));
        when(spring.findById("t1")).thenReturn(Optional.of(doc("CLIENT")));
        when(spring.findByCommandeId("o1")).thenReturn(List.of(doc("CLIENT"), doc("SERVICE")));
        when(spring.findByType("CLIENT")).thenReturn(List.of(doc("CLIENT")));
        when(spring.findAll()).thenReturn(List.of(doc("CLIENT")));

        Ticket t = adapter.save(Ticket.creer("o1", "CMD-1", TicketType.CLIENT, "contenu"));
        assertEquals(TicketType.CLIENT, t.getType());
        assertTrue(adapter.findById("t1").isPresent());
        assertEquals(2, adapter.findByCommande("o1").size());
        assertEquals(1, adapter.findByType(TicketType.CLIENT).size());
        assertEquals(1, adapter.findAll().size());
    }
}
