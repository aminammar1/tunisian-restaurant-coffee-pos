package tn.cafe.pos.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tn.cafe.pos.application.dto.*;
import tn.cafe.pos.domain.exception.UnauthorizedException;
import tn.cafe.pos.domain.model.User;
import tn.cafe.pos.domain.repository.UserRepository;
import tn.cafe.pos.infrastructure.security.JwtService;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users; this.encoder = encoder; this.jwt = jwt;
    }

    public AuthResponse login(LoginRequest req) {
        User u = users.findByUsername(req.username())
                .orElseThrow(() -> new UnauthorizedException("Identifiants invalides"));
        verifierActif(u);
        if (!encoder.matches(req.password(), u.getPasswordHash()))
            throw new UnauthorizedException("Identifiants invalides");
        return token(u);
    }

    public AuthResponse loginPin(PinRequest req) {
        User u = users.findByUsername(req.username())
                .orElseThrow(() -> new UnauthorizedException("PIN invalide"));
        verifierActif(u);
        if (u.getPinHash() == null || !encoder.matches(req.pin(), u.getPinHash()))
            throw new UnauthorizedException("PIN invalide");
        return token(u);
    }

    /** Login gérant par scan QR personnel (caméra téléphone). */
    public AuthResponse loginQr(QrLoginRequest req) {
        User u = users.findByQrKey(req.qrKey())
                .orElseThrow(() -> new UnauthorizedException("Clé QR inconnue"));
        verifierActif(u);
        return token(u);
    }

    private void verifierActif(User u) {
        if (!u.isActif()) throw new UnauthorizedException("Compte désactivé");
    }

    private AuthResponse token(User u) {
        return new AuthResponse(jwt.generer(u.getUsername(), u.getRole().name()), u.getUsername(), u.getRole().name());
    }
}
