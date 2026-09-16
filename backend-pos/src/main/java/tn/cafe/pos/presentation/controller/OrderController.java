package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.cafe.pos.application.dto.CreateOrderRequest;
import tn.cafe.pos.application.service.OrderService;
import tn.cafe.pos.domain.model.Order;
import tn.cafe.pos.domain.model.OrderStatus;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Commandes", description = "Client crée, gérant reçoit en temps réel")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @PostMapping
    @Operation(summary = "Créer commande (client)")
    public ResponseEntity<Order> creer(@Valid @RequestBody CreateOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(req));
    }

    @GetMapping
    @Operation(summary = "Lister commandes (?statut=EN_ATTENTE)")
    public List<Order> lister(@RequestParam(required = false) OrderStatus statut) {
        if (statut != null) return service.parStatut(statut);
        return service.toutes();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail commande")
    public Order parId(@PathVariable String id) { return service.parId(id); }

    @PatchMapping("/{id}/statut")
    @Operation(summary = "Changer statut (gérant) : CONFIRMEE, EN_PREPARATION, PRETE...")
    public Order statut(@PathVariable String id, @RequestBody Map<String, OrderStatus> body) {
        return service.changerStatut(id, body.get("statut"));
    }

    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annuler commande")
    public Order annuler(@PathVariable String id) { return service.annuler(id); }
}
