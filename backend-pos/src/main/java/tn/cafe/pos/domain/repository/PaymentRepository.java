package tn.cafe.pos.domain.repository;

import tn.cafe.pos.domain.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment p);
    Optional<Payment> findById(String id);
    List<Payment> findByCommande(String commandeId);
    List<Payment> findAll();
}
