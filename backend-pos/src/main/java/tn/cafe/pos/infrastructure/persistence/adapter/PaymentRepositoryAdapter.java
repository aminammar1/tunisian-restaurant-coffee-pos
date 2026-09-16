package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import tn.cafe.pos.domain.model.Payment;
import tn.cafe.pos.domain.repository.PaymentRepository;
import tn.cafe.pos.infrastructure.persistence.mapper.Mappers;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringPaymentMongo;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {
    private final SpringPaymentMongo spring;
    public PaymentRepositoryAdapter(SpringPaymentMongo spring) { this.spring = spring; }

    @Override public Payment save(Payment p) { return Mappers.toPayment(spring.save(Mappers.toPaymentDoc(p))); }
    @Override public Optional<Payment> findById(String id) { return spring.findById(id).map(Mappers::toPayment); }
    @Override public List<Payment> findByCommande(String commandeId) {
        return spring.findByCommandeId(commandeId).stream().map(Mappers::toPayment).toList();
    }
    @Override public List<Payment> findAll() { return spring.findAll().stream().map(Mappers::toPayment).toList(); }
}
