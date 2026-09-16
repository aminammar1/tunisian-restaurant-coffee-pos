package tn.cafe.pos.application.service;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import tn.cafe.pos.application.dto.*;
import tn.cafe.pos.domain.exception.UnauthorizedException;
import tn.cafe.pos.domain.model.Role;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.domain.repository.UserRepository;
import tn.cafe.pos.infrastructure.config.AppProperties;
import tn.cafe.pos.infrastructure.security.JwtService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    JwtService jwt;
    AuthService service;

    @BeforeEach
    void setUp() {
        jwt = new JwtService(new AppProperties(
                new AppProperties.Jwt("test-secret-min-32-chars-0123456789abcdef", 3600000),
                null, null, null, null));
        service = new AuthService(users, encoder, jwt);
    }

    private User user() {
        return new User("1", "admin", encoder.encode("admin123@"),
                encoder.encode("1234"), "QR-ABC", Role.GERANT, true, null);
    }

    @Test
    void login_ok() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(user()));
        AuthResponse r = service.login(new LoginRequest("admin", "admin123@"));
        assertNotNull(r.token());
        assertEquals("admin", r.username());
    }

    @Test
    void login_mauvais_mot_de_passe() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(user()));
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("admin", "faux")));
    }

    @Test
    void login_pin_ok_defaut_1234() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(user()));
        AuthResponse r = service.loginPin(new PinRequest("admin", "1234"));
        assertNotNull(r.token());
    }

    @Test
    void login_pin_faux() {
        when(users.findByUsername("admin")).thenReturn(Optional.of(user()));
        assertThrows(UnauthorizedException.class, () -> service.loginPin(new PinRequest("admin", "9999")));
    }

    @Test
    void login_qr_ok() {
        when(users.findByQrKey("QR-ABC")).thenReturn(Optional.of(user()));
        AuthResponse r = service.loginQr(new QrLoginRequest("QR-ABC"));
        assertEquals("GERANT", r.role());
    }

    @Test
    void login_qr_inconnu() {
        when(users.findByQrKey(anyString())).thenReturn(Optional.empty());
        assertThrows(UnauthorizedException.class, () -> service.loginQr(new QrLoginRequest("QR-X")));
    }

    @Test
    void compte_desactive_refuse() {
        User u = user(); u.desactiver();
        when(users.findByUsername("admin")).thenReturn(Optional.of(u));
        assertThrows(UnauthorizedException.class, () -> service.login(new LoginRequest("admin", "admin123@")));
    }
}
