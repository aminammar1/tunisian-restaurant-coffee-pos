package tn.cafe.pos.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.Payment;
import tn.cafe.pos.domain.model.PaymentType;
import tn.cafe.pos.infrastructure.persistence.document.PaymentDocument;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringPaymentMongo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRepositoryAdapterTest {
    @Mock SpringPaymentMongo spring;
    @InjectMocks PaymentRepositoryAdapter adapter;

    private PaymentDocument doc() {
        PaymentDocument d = new PaymentDocument();
        d.id = "pay1"; d.commandeId = "o1"; d.type = "QR";
        d.montant = new BigDecimal("5.000"); d.statut = "REUSSI"; d.referenceSimulee = "SIM-QR-X";
        return d;
    }

    @Test
    void save_find() {
        when(spring.save(any())).thenReturn(doc());
        when(spring.findById("pay1")).thenReturn(Optional.of(doc()));
        when(spring.findByCommandeId("o1")).thenReturn(List.of(doc()));
        when(spring.findAll()).thenReturn(List.of(doc()));

        Payment p = adapter.save(Payment.simulerSucces("o1", PaymentType.QR, new BigDecimal("5.000"), "SIM-QR-X"));
        assertTrue(p.estReussi());
        assertTrue(adapter.findById("pay1").isPresent());
        assertEquals(1, adapter.findByCommande("o1").size());
        assertEquals(1, adapter.findAll().size());
    }
}
