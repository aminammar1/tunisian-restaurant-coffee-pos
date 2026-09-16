package tn.cafe.pos.application.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import tn.cafe.pos.application.dto.PaymentRequest;
import tn.cafe.pos.application.dto.ProximityRequest;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.domain.repository.OrderRepository;
import tn.cafe.pos.domain.repository.PaymentRepository;
import tn.cafe.pos.infrastructure.realtime.OrderSseHub;

@Service
public class PaymentService {
    private final OrderRepository commandes;
    private final PaymentRepository paiements;
    private final TicketService tickets;
    private final OrderSseHub hub;

    public PaymentService(OrderRepository commandes, PaymentRepository paiements,
                          TicketService tickets, OrderSseHub hub) {
        this.commandes = commandes; this.paiements = paiements;
        this.tickets = tickets; this.hub = hub;
    }

    /**
     * Paiement 100% simulé : QR / APPLE_PAY / CARD / CASH / INFRARED => REUSSI.
     * Déclenche : commande PAYEE + 2 tickets automatiques.
     */
    public PaiementResultat confirmer(PaymentRequest req) {
        Order o = commandes.findById(req.commandeId())
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));
        if (o.getStatut() == OrderStatus.PAYEE) throw new BusinessException("Commande déjà payée");
        if (o.getStatut() == OrderStatus.ANNULEE) throw new BusinessException("Commande annulée");

        String ref = "SIM-" + req.type().name() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Payment p = paiements.save(Payment.simulerSucces(o.getId(), req.type(), o.getTotal(), ref));
        o.marquerPayee();
        Order payee = commandes.save(o);
        List<Ticket> deux = tickets.genererDeuxTickets(payee);
        hub.diffuser(payee);
        return new PaiementResultat(p, payee, deux);
    }

    /** Détection infrarouge/NFC simulée : téléphone/carte présenté => auto-confirmation. */
    public PaiementResultat confirmerProximite(ProximityRequest req) {
        boolean detecte = req.signalDetecte() == null || req.signalDetecte();
        if (!detecte) throw new BusinessException("Aucun signal infrarouge/NFC détecté");
        return confirmer(new PaymentRequest(req.commandeId(), PaymentType.INFRARED));
    }

    public List<Payment> parCommande(String commandeId) { return paiements.findByCommande(commandeId); }
    public List<Payment> tous() { return paiements.findAll(); }

    public record PaiementResultat(Payment paiement, Order commande, List<Ticket> tickets) {}
}
