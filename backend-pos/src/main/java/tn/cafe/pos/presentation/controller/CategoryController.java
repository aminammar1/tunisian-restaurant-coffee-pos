package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.cafe.pos.application.dto.CategoryRequest;
import tn.cafe.pos.application.service.CategoryService;
import tn.cafe.pos.domain.model.Category;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Catégories", description = "Gestion gérant : Café, Boissons, Plats, Desserts...")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Lister catégories (public)")
    public List<Category> lister() { return service.lister(); }

    @GetMapping("/actives")
    @Operation(summary = "Lister catégories actives (client)")
    public List<Category> actives() { return service.listerActives(); }

    @GetMapping("/{id}")
    @Operation(summary = "Détail catégorie")
    public Category parId(@PathVariable String id) { return service.parId(id); }

    @PostMapping
    @Operation(summary = "Créer catégorie (gérant)")
    public ResponseEntity<Category> creer(@Valid @RequestBody CategoryRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(req));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier catégorie (gérant)")
    public Category modifier(@PathVariable String id, @Valid @RequestBody CategoryRequest req) {
        return service.modifier(id, req);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer catégorie (gérant)")
    public ResponseEntity<Void> supprimer(@PathVariable String id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
