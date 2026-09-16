package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.OrderStatus;
import tn.cafe.pos.domain.repository.OrderRepository;
import tn.cafe.pos.infrastructure.persistence.mapper.Mappers;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringOrderMongo;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {
    private final SpringOrderMongo spring;
    public OrderRepositoryAdapter(SpringOrderMongo spring) { this.spring = spring; }

    @Override public Order save(Order o) { return Mappers.toOrder(spring.save(Mappers.toOrderDoc(o))); }
    @Override public Optional<Order> findById(String id) { return spring.findById(id).map(Mappers::toOrder); }
    @Override public List<Order> findAll() { return spring.findAll().stream().map(Mappers::toOrder).toList(); }
    @Override public List<Order> findByStatut(OrderStatus s) {
        return spring.findByStatut(s.name()).stream().map(Mappers::toOrder).toList();
    }
    @Override public void deleteById(String id) { spring.deleteById(id); }
    @Override public long count() { return spring.count(); }
}
