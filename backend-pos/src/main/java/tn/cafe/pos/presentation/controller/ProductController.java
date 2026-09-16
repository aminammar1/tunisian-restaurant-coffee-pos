package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.cafe.pos.application.dto.ProductRequest;
import tn.cafe.pos.application.service.ProductService;
import tn.cafe.pos.domain.model.Product;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Produits", description = "Catalogue : nom, prix TND, catégorie, image, disponible")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Lister produits (?categorieId=, ?disponibles=true)")
    public List<Product> lister(@RequestParam(required = false) String categorieId,
                                @RequestParam(required = false) Boolean disponibles) {
        if (Boolean.TRUE.equals(disponibles)) return service.disponibles();
        if (categorieId != null) return service.parCategorie(categorieId);
        return service.lister();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail produit")
    public Product parId(@PathVariable String id) { return service.parId(id); }

    @PostMapping
    @Operation(summary = "Créer produit (gérant)")
    public ResponseEntity<Product> creer(@Valid @RequestBody ProductRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier produit (gérant)")
    public Product modifier(@PathVariable String id, @Valid @RequestBody ProductRequest req) {
        return service.modifier(id, req);
    }

    @PatchMapping("/{id}/disponibilite")
    @Operation(summary = "Disponible / indisponible (gérant)")
    public Product dispo(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        return service.changerDisponibilite(id, Boolean.TRUE.equals(body.get("disponible")));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer produit (gérant)")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
