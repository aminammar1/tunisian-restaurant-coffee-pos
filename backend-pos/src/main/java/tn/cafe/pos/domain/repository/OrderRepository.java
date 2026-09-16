package tn.cafe.pos.domain.repository;

import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.OrderStatus;
import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    Order save(Order o);
    Optional<Order> findById(String id);
    List<Order> findAll();
    List<Order> findByStatut(OrderStatus statut);
    void deleteById(String id);
    long count();
}
