package tn.cafe.pos.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.infrastructure.persistence.document.OrderDocument;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringOrderMongo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryAdapterTest {
    @Mock SpringOrderMongo spring;
    @InjectMocks OrderRepositoryAdapter adapter;

    private OrderDocument doc() {
        OrderDocument d = new OrderDocument();
        d.id = "o1"; d.numero = "CMD-000001"; d.total = new BigDecimal("5.000");
        d.statut = "EN_ATTENTE"; d.tableOuClient = "T1";
        OrderDocument.Item i = new OrderDocument.Item();
        i.produitId = "p1"; i.nomProduit = "Espresso";
        i.prixUnitaire = new BigDecimal("2.500"); i.quantite = 2;
        d.items = List.of(i);
        return d;
    }

    @Test
    void save_find_count() {
        when(spring.save(any())).thenReturn(doc());
        when(spring.findById("o1")).thenReturn(Optional.of(doc()));
        when(spring.findAll()).thenReturn(List.of(doc()));
        when(spring.findByStatut("EN_ATTENTE")).thenReturn(List.of(doc()));
        when(spring.count()).thenReturn(1L);

        Order o = adapter.save(Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.500"), 2)), "T1"));
        assertEquals("CMD-000001", o.getNumero());
        assertTrue(adapter.findById("o1").isPresent());
        assertEquals(1, adapter.findAll().size());
        assertEquals(1, adapter.findByStatut(OrderStatus.EN_ATTENTE).size());
        assertEquals(1L, adapter.count());
    }
}
