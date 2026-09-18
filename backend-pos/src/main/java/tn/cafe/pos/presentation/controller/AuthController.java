package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.cafe.pos.application.dto.*;
import tn.cafe.pos.application.service.AuthService;
import tn.cafe.pos.application.service.QrKeyService;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentification", description = "Login gérant : mot de passe, PIN 1234 ou QR personnel")
public class AuthController {
    private final AuthService auth;
    private final QrKeyService qrKeys;

    public AuthController(AuthService auth, QrKeyService qrKeys) {
        this.auth = java.util.Objects.requireNonNull(auth);
        this.qrKeys = java.util.Objects.requireNonNull(qrKeys);
    }

    @PostMapping("/login")
    @Operation(summary = "Login username/password (admin / admin123@)")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(auth.login(req));
    }

    @PostMapping("/pin")
    @Operation(summary = "Login code PIN (défaut 1234)")
    public ResponseEntity<AuthResponse> pin(@Valid @RequestBody PinRequest req) {
        return ResponseEntity.ok(auth.loginPin(req));
    }

    @PostMapping("/qr")
    @Operation(summary = "Login gérant par scan QR personnel")
    public ResponseEntity<AuthResponse> qr(@Valid @RequestBody QrLoginRequest req) {
        return ResponseEntity.ok(auth.loginQr(req));
    }

    @GetMapping("/qr-key")
    @Operation(summary = "Générer le badge QR du gérant")
    public ResponseEntity<java.util.Map<String, String>> qrKey(
            @RequestParam(defaultValue = "admin") String username) {
        return ResponseEntity.ok(java.util.Map.of("qrKey", qrKeys.cle(username)));
    }
}
