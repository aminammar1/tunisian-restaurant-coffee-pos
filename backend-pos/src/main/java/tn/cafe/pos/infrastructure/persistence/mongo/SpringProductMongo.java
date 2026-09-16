package tn.cafe.pos.infrastructure.persistence.mongo;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.cafe.pos.infrastructure.persistence.document.ProductDocument;

public interface SpringProductMongo extends MongoRepository<ProductDocument, String> {
    List<ProductDocument> findByCategorieId(String categorieId);
    List<ProductDocument> findByDisponibleTrue();
}
