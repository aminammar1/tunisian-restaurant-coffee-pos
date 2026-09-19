package tn.cafe.pos.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.application.dto.CreateOrderRequest;
import tn.cafe.pos.application.dto.OrderItemRequest;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.domain.repository.OrderRepository;
import tn.cafe.pos.domain.repository.ProductRepository;
import tn.cafe.pos.infrastructure.realtime.OrderSseHub;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock OrderRepository commandes;
    @Mock ProductRepository produits;
    @Mock OrderSseHub hub;
    OrderService service;

    @BeforeEach void setUp() { service = new OrderService(commandes, produits, hub); }

    private Product espresso() {
        Product p = Product.creer("Espresso", new BigDecimal("2.500"), "c1", null, null);
        p.setId("p1");
        return p;
    }

    @Test
    void creer_calcule_total_et_notifie() {
        when(produits.findById("p1")).thenReturn(Optional.of(espresso()));
        when(commandes.count()).thenReturn(0L);
        when(commandes.save(any())).thenAnswer(i -> i.getArgument(0));
        Order o = service.creer(new CreateOrderRequest(List.of(new OrderItemRequest("p1", 2)), "T3"));
        assertEquals(new BigDecimal("5.000"), o.getTotal());
        assertEquals("CMD-000001", o.getNumero());
        verify(hub).diffuserCreation(any());
    }

    @Test
    void creer_produit_indisponible() {
        Product p = espresso(); p.marquerDisponible(false);
        when(produits.findById("p1")).thenReturn(Optional.of(p));
        assertThrows(BusinessException.class, () ->
                service.creer(new CreateOrderRequest(List.of(new OrderItemRequest("p1", 1)), null)));
    }

    @Test
    void creer_quantite_invalide_refusee_en_400() {
        assertThrows(BusinessException.class, () ->
                service.creer(new CreateOrderRequest(List.of(new OrderItemRequest("p1", 0)), "T3")));
        verify(commandes, never()).save(any());
        verify(hub, never()).diffuserCreation(any());
    }

    @Test
    void changerStatut_et_annuler() {
        Order o = Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.5"), 1)), null);
        o.setId("o1");
        when(commandes.findById("o1")).thenReturn(Optional.of(o));
        when(commandes.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(OrderStatus.EN_PREPARATION, service.changerStatut("o1", OrderStatus.EN_PREPARATION).getStatut());
        assertEquals(OrderStatus.ANNULEE, service.annuler("o1").getStatut());
    }

    @Test
    void changerStatut_ne_peut_pas_marquer_payee_sans_paiement() {
        Order o = Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.5"), 1)), null);
        o.setId("o1");
        when(commandes.findById("o1")).thenReturn(Optional.of(o));

        assertThrows(IllegalStateException.class, () -> service.changerStatut("o1", OrderStatus.PAYEE));
        verify(commandes, never()).save(any());
    }
}
