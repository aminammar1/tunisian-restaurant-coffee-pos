package tn.cafe.pos.infrastructure.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.cafe.pos.infrastructure.persistence.document.OrderDocument;

public interface SpringOrderMongo extends MongoRepository<OrderDocument, String> {
    List<OrderDocument> findByStatut(String statut);
}
