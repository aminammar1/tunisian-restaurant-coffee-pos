package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.infrastructure.persistence.document.UserDocument;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringUserMongo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {
    @Mock SpringUserMongo spring;
    @InjectMocks UserRepositoryAdapter adapter;

    private UserDocument doc() {
        UserDocument d = new UserDocument();
        d.id = "1"; d.username = "admin"; d.passwordHash = "h";
        d.pinHash = "p"; d.qrKey = "QR-X"; d.role = "GERANT"; d.actif = true;
        return d;
    }

    @Test
    void save_find_exists() {
        when(spring.save(any())).thenReturn(doc());
        when(spring.findById("1")).thenReturn(Optional.of(doc()));
        when(spring.findByUsername("admin")).thenReturn(Optional.of(doc()));
        when(spring.findByQrKey("QR-X")).thenReturn(Optional.of(doc()));
        when(spring.existsByUsername("admin")).thenReturn(true);
        when(spring.findAll()).thenReturn(List.of(doc()));

        User u = adapter.save(User.nouveauGerant("admin", "h", "p", "QR-X"));
        assertEquals("admin", u.getUsername());
        assertTrue(adapter.findById("1").isPresent());
        assertTrue(adapter.findByUsername("admin").isPresent());
        assertTrue(adapter.findByQrKey("QR-X").isPresent());
        assertTrue(adapter.existsByUsername("admin"));
        assertEquals(1, adapter.findAll().size());
    }
}
