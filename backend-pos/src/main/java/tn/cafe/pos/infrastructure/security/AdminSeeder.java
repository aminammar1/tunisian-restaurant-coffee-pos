package tn.cafe.pos.infrastructure.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.cafe.pos.domain.model.Role;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.domain.repository.UserRepository;
import tn.cafe.pos.infrastructure.config.AppProperties;
import tn.cafe.pos.infrastructure.qr.QrKeyGenerator;

/** Crée admin/admin123@ + PIN 1234 + QR key au premier démarrage. */
@Component
public class AdminSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AppProperties props;
    private final QrKeyGenerator qr;

    public AdminSeeder(UserRepository users, PasswordEncoder encoder, AppProperties props, QrKeyGenerator qr) {
        this.users = users; this.encoder = encoder; this.props = props; this.qr = qr;
    }

    @Override
    public void run(String... args) {
        String username = props.admin().username();
        if (users.existsByUsername(username)) return;
        String qrKey = props.admin().qrKey();
        if (qrKey == null || qrKey.isBlank() || qrKey.contains("CHANGE-ME")) qrKey = qr.generer();
        User admin = new User(null, username,
                encoder.encode(props.admin().password()),
                encoder.encode(props.admin().pin()),
                qrKey, Role.GERANT, true, java.time.Instant.now());
        users.save(admin);
        // Never print a reusable QR credential in a terminal log.
        log.info("[auth] manager account '{}' created (PIN and QR credentials are configured securely)", username);
    }
}
