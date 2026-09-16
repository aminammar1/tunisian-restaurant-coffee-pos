package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import tn.cafe.pos.domain.model.Ticket;
import tn.cafe.pos.domain.model.TicketType;
import tn.cafe.pos.domain.repository.TicketRepository;
import tn.cafe.pos.infrastructure.persistence.mapper.Mappers;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringTicketMongo;

@Repository
public class TicketRepositoryAdapter implements TicketRepository {
    private final SpringTicketMongo spring;
    public TicketRepositoryAdapter(SpringTicketMongo spring) { this.spring = spring; }

    @Override public Ticket save(Ticket t) { return Mappers.toTicket(spring.save(Mappers.toTicketDoc(t))); }
    @Override public Optional<Ticket> findById(String id) { return spring.findById(id).map(Mappers::toTicket); }
    @Override public List<Ticket> findByCommande(String commandeId) {
        return spring.findByCommandeId(commandeId).stream().map(Mappers::toTicket).toList();
    }
    @Override public List<Ticket> findByType(TicketType type) {
        return spring.findByType(type.name()).stream().map(Mappers::toTicket).toList();
    }
    @Override public List<Ticket> findAll() { return spring.findAll().stream().map(Mappers::toTicket).toList(); }
}
