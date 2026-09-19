package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.file.Files;
import java.util.Map;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tn.cafe.pos.infrastructure.storage.ImageStorageService;

@RestController
@RequestMapping("/api/v1/images")
@Tag(name = "Images", description = "Photos du catalogue importées depuis le poste du gérant")
public class ImageController {
    private final ImageStorageService stockage;

    public ImageController(ImageStorageService stockage) { this.stockage = stockage; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importer une photo (gérant) : jpg, png, webp, gif, 5 Mo max")
    public ResponseEntity<Map<String, String>> importer(@RequestParam("fichier") MultipartFile fichier) {
        String nom = stockage.stocker(fichier);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("nom", nom, "url", "/api/v1/images/" + nom));
    }

    @GetMapping("/{nom:.+}")
    @Operation(summary = "Lire une photo importée (public, affichée par les caisses)")
    public ResponseEntity<Resource> lire(@PathVariable String nom) throws Exception {
        var chemin = stockage.charger(nom);
        var ressource = new FileSystemResource(chemin);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stockage.typeContenu(nom)))
                .contentLength(Files.size(chemin))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(ressource);
    }
}
