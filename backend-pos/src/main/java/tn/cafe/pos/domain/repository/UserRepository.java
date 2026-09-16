package tn.cafe.pos.domain.repository;

import tn.cafe.pos.domain.model.User;
import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User u);
    Optional<User> findById(String id);
    Optional<User> findByUsername(String username);
    Optional<User> findByQrKey(String qrKey);
    List<User> findAll();
    void deleteById(String id);
    boolean existsByUsername(String username);
}
