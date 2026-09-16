package tn.cafe.pos.domain.repository;

import tn.cafe.pos.domain.model.Ticket;
import tn.cafe.pos.domain.model.TicketType;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {
    Ticket save(Ticket t);
    Optional<Ticket> findById(String id);
    List<Ticket> findByCommande(String commandeId);
    List<Ticket> findByType(TicketType type);
    List<Ticket> findAll();
}
