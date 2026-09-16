package tn.cafe.pos.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.Ticket;
import tn.cafe.pos.domain.model.TicketType;
import tn.cafe.pos.domain.repository.TicketRepository;
import tn.cafe.pos.infrastructure.printing.TicketRenderer;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final TicketRenderer renderer;

    public TicketService(TicketRepository tickets, TicketRenderer renderer) {
        this.tickets = tickets; this.renderer = renderer;
    }

    /** Génère et persiste les 2 tickets obligatoires : SERVICE + CLIENT. */
    public List<Ticket> genererDeuxTickets(Order order) {
        Ticket service = tickets.save(Ticket.creer(order.getId(), order.getNumero(),
                TicketType.SERVICE, renderer.rendre(order, TicketType.SERVICE)));
        Ticket client = tickets.save(Ticket.creer(order.getId(), order.getNumero(),
                TicketType.CLIENT, renderer.rendre(order, TicketType.CLIENT)));
        return List.of(service, client);
    }

    public List<Ticket> parCommande(String commandeId) { return tickets.findByCommande(commandeId); }
    public List<Ticket> tous() { return tickets.findAll(); }
    public Ticket parId(String id) {
        return tickets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable"));
    }
}
