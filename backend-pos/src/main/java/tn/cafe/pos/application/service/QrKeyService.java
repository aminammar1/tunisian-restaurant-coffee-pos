package tn.cafe.pos.application.service;

import org.springframework.stereotype.Service;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.domain.repository.UserRepository;
import tn.cafe.pos.infrastructure.qr.QrKeyGenerator;

@Service
public class QrKeyService {
    private final UserRepository users;
    private final QrKeyGenerator generator;

    public QrKeyService(UserRepository users, QrKeyGenerator generator) {
        this.users = users; this.generator = generator;
    }

    public User regenerer(String username) {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));
        u.regenererQrKey(generator.generer());
        return users.save(u);
    }
}
