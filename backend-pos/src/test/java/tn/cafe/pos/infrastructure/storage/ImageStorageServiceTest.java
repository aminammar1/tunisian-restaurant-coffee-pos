package tn.cafe.pos.infrastructure.storage;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;

class ImageStorageServiceTest {
    @TempDir Path tmp;

    private ImageStorageService stockage() {
        return new ImageStorageService(tmp.resolve("images"), 1024);
    }

    private static MockMultipartFile png(String nom, byte[] contenu) {
        return new MockMultipartFile("fichier", nom, "image/png", contenu);
    }

    @Test
    void stocker_genere_uuid_et_ecrit_le_fichier() throws Exception {
        ImageStorageService s = stockage();
        String nom = s.stocker(png("café.png", new byte[]{1, 2, 3}));
        assertTrue(nom.endsWith(".png"), "extension conservée : " + nom);
        assertTrue(Files.isRegularFile(s.charger(nom)));
        assertEquals("image/png", s.typeContenu(nom));
    }

    @Test
    void stocker_rejette_format_non_image() {
        ImageStorageService s = stockage();
        assertThrows(BusinessException.class,
                () -> s.stocker(new MockMultipartFile("fichier", "note.txt", "text/plain", new byte[]{1})));
    }

    @Test
    void stocker_rejette_fichier_trop_lourd() {
        ImageStorageService s = stockage();
        assertThrows(BusinessException.class, () -> s.stocker(png("gros.png", new byte[2048])));
    }

    @Test
    void stocker_rejette_fichier_vide() {
        ImageStorageService s = stockage();
        assertThrows(BusinessException.class, () -> s.stocker(png("vide.png", new byte[0])));
    }

    @Test
    void extension_resolue_depuis_le_type_contenu_sans_extension() {
        ImageStorageService s = stockage();
        String nom = s.stocker(new MockMultipartFile("fichier", "photo", "image/jpeg", new byte[]{9}));
        assertTrue(nom.endsWith(".jpg"), nom);
    }

    @Test
    void charger_bloque_la_remontee_de_dossier() {
        ImageStorageService s = stockage();
        assertThrows(ResourceNotFoundException.class, () -> s.charger("../application.properties"));
        assertThrows(ResourceNotFoundException.class, () -> s.charger("inexistant.png"));
    }
}
