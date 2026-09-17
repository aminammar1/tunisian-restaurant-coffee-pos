package tn.cafe.pos.presentation.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tn.cafe.pos.application.dto.AuthResponse;
import tn.cafe.pos.application.dto.PaymentRequest;
import tn.cafe.pos.application.service.AuthService;
import tn.cafe.pos.application.service.PaymentService;
import tn.cafe.pos.application.service.ProductService;
import tn.cafe.pos.application.service.QrKeyService;
import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.OrderItem;
import tn.cafe.pos.domain.model.Payment;
import tn.cafe.pos.domain.model.PaymentType;
import tn.cafe.pos.domain.model.Product;
import tn.cafe.pos.domain.model.Ticket;
import tn.cafe.pos.domain.model.TicketType;
import tn.cafe.pos.presentation.advice.GlobalExceptionHandler;

/**
 * Contract-level checks for payloads the JavaFX client sends.  This avoids a MongoDB
 * dependency while exercising Spring request mapping, JSON names, and validation.
 */
@ExtendWith(MockitoExtension.class)
class ApiContractControllerTest {
    @Mock private AuthService auth;
    @Mock private QrKeyService qrKeys;
    @Mock private PaymentService payments;
    @Mock private ProductService products;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(
                        new AuthController(auth, qrKeys),
                        new PaymentController(payments),
                        new ProductController(products))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void pinLogin_requiresAndForwardsUsernameAndPin() throws Exception {
        when(auth.loginPin(any())).thenReturn(new AuthResponse("jwt", "admin", "GERANT"));

        mvc.perform(post("/api/v1/auth/pin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"pin\":\"1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.username").value("admin"));

        verify(auth).loginPin(argThat(request -> "admin".equals(request.username()) && "1234".equals(request.pin())));
    }

    @Test
    void directPayment_returnsPersistedPaymentAndExactlyTwoTickets() throws Exception {
        Order order = Order.creer(List.of(new OrderItem("p1", "Espresso", new BigDecimal("2.500"), 1)), "T1");
        order.setId("o1");
        order.setNumero("CMD-000001");
        order.marquerPayee();
        Payment payment = Payment.simulerSucces("o1", PaymentType.CASH, new BigDecimal("2.500"), "SIM-CASH-1");
        List<Ticket> tickets = List.of(
                Ticket.creer("o1", "CMD-000001", TicketType.SERVICE, "cuisine"),
                Ticket.creer("o1", "CMD-000001", TicketType.CLIENT, "recu"));
        when(payments.confirmer(new PaymentRequest("o1", PaymentType.CASH)))
                .thenReturn(new PaymentService.PaiementResultat(payment, order, tickets));

        mvc.perform(post("/api/v1/payments/confirmer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"commandeId\":\"o1\",\"type\":\"CASH\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paiement.type").value("CASH"))
                .andExpect(jsonPath("$.commande.statut").value("PAYEE"))
                .andExpect(jsonPath("$.tickets", hasSize(2)));

        verify(payments).confirmer(new PaymentRequest("o1", PaymentType.CASH));
    }

    @Test
    void availabilityPatch_rejectsMissingBooleanInsteadOfSilentlyMakingProductUnavailable() throws Exception {
        mvc.perform(patch("/api/v1/products/p1/disponibilite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreur").exists());

        verifyNoInteractions(products);
    }

    @Test
    void availabilityPatch_forwardsTheExplicitBoolean() throws Exception {
        Product espresso = Product.creer("Espresso", new BigDecimal("2.500"), "c1", null, null);
        espresso.setId("p1");
        when(products.changerDisponibilite("p1", true)).thenReturn(espresso);

        mvc.perform(patch("/api/v1/products/p1/disponibilite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"disponible\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));

        verify(products).changerDisponibilite("p1", true);
    }
}
