package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import tn.cafe.pos.application.service.TicketService;
import tn.cafe.pos.domain.model.Ticket;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Tickets", description = "2 tickets auto par paiement : SERVICE (cuisine) + CLIENT (reçu)")
public class TicketController {
    private final TicketService service;
    public TicketController(TicketService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Lister tickets (?commandeId=)")
    public List<Ticket> lister(@RequestParam(required = false) String commandeId) {
        if (commandeId != null) return service.parCommande(commandeId);
        return service.tous();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail ticket (contenu prêt à imprimer)")
    public Ticket parId(@PathVariable String id) { return service.parId(id); }
}
