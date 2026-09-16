package tn.cafe.pos.infrastructure.persistence.mongo;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import tn.cafe.pos.infrastructure.persistence.document.*;

public interface SpringUserMongo extends MongoRepository<UserDocument, String> {
    Optional<UserDocument> findByUsername(String username);
    Optional<UserDocument> findByQrKey(String qrKey);
    boolean existsByUsername(String username);
}
