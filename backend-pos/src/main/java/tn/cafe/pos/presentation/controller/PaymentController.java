package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.cafe.pos.application.dto.PaymentRequest;
import tn.cafe.pos.application.dto.ProximityRequest;
import tn.cafe.pos.application.service.PaymentService;
import tn.cafe.pos.domain.model.Payment;

@RestController
@RequestMapping("/api/v1/payments")
@Tag(name = "Paiements (simulés)", description = "QR, Apple Pay, Carte, Cash, Infrarouge. Aucun débit réel. Paiement => 2 tickets auto.")
public class PaymentController {
    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @PostMapping("/confirmer")
    @Operation(summary = "Confirmer paiement simulé + générer 2 tickets")
    public ResponseEntity<Map<String, Object>> confirmer(@Valid @RequestBody PaymentRequest req) {
        var r = service.confirmer(req);
        return ResponseEntity.ok(Map.of(
                "paiement", r.paiement(),
                "commande", r.commande(),
                "tickets", r.tickets()));
    }

    @PostMapping("/proximite")
    @Operation(summary = "Auto-confirmation infrarouge/NFC simulée (téléphone/carte présenté)")
    public ResponseEntity<Map<String, Object>> proximite(@Valid @RequestBody ProximityRequest req) {
        var r = service.confirmerProximite(req);
        return ResponseEntity.ok(Map.of(
                "paiement", r.paiement(),
                "commande", r.commande(),
                "tickets", r.tickets()));
    }

    @GetMapping
    @Operation(summary = "Lister paiements (?commandeId=)")
    public List<Payment> lister(@RequestParam(required = false) String commandeId) {
        if (commandeId != null) return service.parCommande(commandeId);
        return service.tous();
    }
}
