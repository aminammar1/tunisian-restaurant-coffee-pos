package tn.cafe.pos.infrastructure.persistence.mongo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.cafe.pos.infrastructure.persistence.document.*;

public interface SpringCategoryMongo extends MongoRepository<CategoryDocument, String> {
    Optional<CategoryDocument> findByNom(String nom);
    List<CategoryDocument> findByActiveTrueOrderByOrdreAsc();
}
