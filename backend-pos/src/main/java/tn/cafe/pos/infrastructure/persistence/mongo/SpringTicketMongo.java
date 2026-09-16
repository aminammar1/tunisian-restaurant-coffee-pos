package tn.cafe.pos.infrastructure.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.cafe.pos.infrastructure.persistence.document.TicketDocument;

public interface SpringTicketMongo extends MongoRepository<TicketDocument, String> {
    List<TicketDocument> findByCommandeId(String commandeId);
    List<TicketDocument> findByType(String type);
}
