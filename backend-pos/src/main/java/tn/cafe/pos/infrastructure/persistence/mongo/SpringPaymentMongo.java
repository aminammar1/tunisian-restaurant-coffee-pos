package tn.cafe.pos.infrastructure.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.cafe.pos.infrastructure.persistence.document.PaymentDocument;

public interface SpringPaymentMongo extends MongoRepository<PaymentDocument, String> {
    List<PaymentDocument> findByCommandeId(String commandeId);
}
