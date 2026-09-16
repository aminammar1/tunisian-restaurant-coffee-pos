package tn.cafe.pos.application.service;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.domain.repository.UserRepository;
import tn.cafe.pos.infrastructure.qr.QrKeyGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QrKeyServiceTest {
    @Mock UserRepository users;
    QrKeyGenerator gen = new QrKeyGenerator();

    @Test
    void regenerer_ok() {
        QrKeyService service = new QrKeyService(users, gen);
        User u = User.nouveauGerant("admin", "h", "p", "OLD");
        when(users.findByUsername("admin")).thenReturn(Optional.of(u));
        when(users.save(any())).thenAnswer(i -> i.getArgument(0));
        User maj = service.regenerer("admin");
        assertNotEquals("OLD", maj.getQrKey());
        assertTrue(maj.getQrKey().startsWith("QR-"));
    }
}
