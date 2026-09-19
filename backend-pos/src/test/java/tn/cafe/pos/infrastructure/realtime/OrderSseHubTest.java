package tn.cafe.pos.infrastructure.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.OrderItem;

/** The compat dispatcher must never announce a payment before it is confirmed. */
@ExtendWith(MockitoExtension.class)
class OrderSseHubTest {
    private static Order commande() {
        return Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.500"), 1)), "T1");
    }

    @Test
    void diffuser_aiguille_creation_tant_que_non_payee() {
        OrderSseHub hub = Mockito.spy(new OrderSseHub());
        lenient().doNothing().when(hub).diffuserCreation(any());
        lenient().doNothing().when(hub).diffuserPaiement(any());

        hub.diffuser(commande());

        verify(hub).diffuserCreation(any());
        verify(hub, Mockito.never()).diffuserPaiement(any());
    }

    @Test
    void diffuser_aiguille_paiement_apres_confirmation() {
        OrderSseHub hub = Mockito.spy(new OrderSseHub());
        lenient().doNothing().when(hub).diffuserCreation(any());
        lenient().doNothing().when(hub).diffuserPaiement(any());

        Order payee = commande();
        payee.marquerPayee();
        hub.diffuser(payee);

        verify(hub).diffuserPaiement(any());
        verify(hub, Mockito.never()).diffuserCreation(any());
    }
}
