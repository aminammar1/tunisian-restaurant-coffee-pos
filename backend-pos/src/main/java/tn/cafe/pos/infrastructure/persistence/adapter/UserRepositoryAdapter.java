package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.domain.repository.UserRepository;
import tn.cafe.pos.infrastructure.persistence.mapper.Mappers;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringUserMongo;

@Repository
public class UserRepositoryAdapter implements UserRepository {
    private final SpringUserMongo spring;
    public UserRepositoryAdapter(SpringUserMongo spring) { this.spring = spring; }

    @Override public User save(User u) {
        var saved = spring.save(Mappers.toUserDoc(u));
        return Mappers.toUser(saved);
    }
    @Override public Optional<User> findById(String id) { return spring.findById(id).map(Mappers::toUser); }
    @Override public Optional<User> findByUsername(String username) {
        return spring.findByUsername(username.strip().toLowerCase()).map(Mappers::toUser);
    }
    @Override public Optional<User> findByQrKey(String qrKey) {
        return spring.findByQrKey(qrKey).map(Mappers::toUser);
    }
    @Override public List<User> findAll() { return spring.findAll().stream().map(Mappers::toUser).toList(); }
    @Override public void deleteById(String id) { spring.deleteById(id); }
    @Override public boolean existsByUsername(String username) { return spring.existsByUsername(username.strip().toLowerCase()); }
}
