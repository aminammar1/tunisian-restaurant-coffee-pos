package tn.cafe.pos.infrastructure.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.infrastructure.config.AppProperties;

/**
 * Stockage local des photos importées depuis le poste du gérant.
 * 100% {@code java.nio.file} (aucun séparateur codé en dur) : fonctionne à
 * l'identique sur Linux, Windows et macOS.
 */
@Service
public class ImageStorageService {
    public static final Set<String> EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Map<String, String> TYPE_VERS_EXT =
            Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "image/gif", "gif");
    private static final Map<String, String> EXT_VERS_TYPE = Map.of(
            "jpg", "image/jpeg", "jpeg", "image/jpeg", "png", "image/png",
            "webp", "image/webp", "gif", "image/gif");

    private final Path dossier;
    private final long maxOctets;

    @Autowired
    public ImageStorageService(AppProperties props) {
        this(Paths.get(props.images().dir()), props.images().maxOctets());
    }

    ImageStorageService(Path dossier, long maxOctets) {
        this.dossier = dossier.toAbsolutePath().normalize();
        this.maxOctets = maxOctets;
        try {
            Files.createDirectories(this.dossier);
        } catch (IOException e) {
            throw new IllegalStateException("Dossier images inaccessible : " + this.dossier, e);
        }
    }

    /** Stocke la photo et renvoie le nom généré (UUID + extension sûre). */
    public String stocker(MultipartFile fichier) {
        if (fichier == null || fichier.isEmpty()) throw new BusinessException("Image vide");
        if (fichier.getSize() > maxOctets) {
            throw new BusinessException("Image trop lourde (max " + (maxOctets / 1024 / 1024) + " Mo)");
        }
        String ext = extensionSure(fichier.getOriginalFilename(), fichier.getContentType());
        String nom = UUID.randomUUID() + "." + ext;
        Path cible = dossier.resolve(nom).normalize();
        if (!cible.startsWith(dossier)) throw new BusinessException("Nom d'image invalide");
        try (var in = fichier.getInputStream()) {
            Files.copy(in, cible, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException("Stockage de l'image impossible");
        }
        return nom;
    }

    /** Chemin du fichier servi, avec garde anti-remontée de dossier. */
    public Path charger(String nom) {
        if (nom == null || nom.isBlank() || nom.contains("..")) {
            throw new ResourceNotFoundException("Image introuvable");
        }
        Path cible = dossier.resolve(nom.strip()).normalize();
        if (!cible.startsWith(dossier) || !Files.isRegularFile(cible)) {
            throw new ResourceNotFoundException("Image introuvable");
        }
        return cible;
    }

    public String typeContenu(String nom) {
        try {
            String sonde = Files.probeContentType(dossier.resolve(nom).normalize());
            if (sonde != null && sonde.startsWith("image/")) return sonde;
        } catch (IOException ignored) {
            // repli sur l'extension ci-dessous
        }
        return EXT_VERS_TYPE.getOrDefault(extensionFichier(nom), "application/octet-stream");
    }

    private static String extensionSure(String nomFichier, String typeContenu) {
        String ext = extensionFichier(nomFichier);
        if (EXTENSIONS.contains(ext)) return ext;
        if (typeContenu != null) {
            String parType = TYPE_VERS_EXT.get(typeContenu.toLowerCase(Locale.ROOT).split(";")[0].strip());
            if (parType != null) return parType;
        }
        throw new BusinessException("Format d'image non supporté (jpg, png, webp, gif)");
    }

    private static String extensionFichier(String nomFichier) {
        if (nomFichier == null) return "";
        String base = Paths.get(nomFichier).getFileName().toString().toLowerCase(Locale.ROOT);
        int point = base.lastIndexOf('.');
        if (point < 0 || point == base.length() - 1) return "";
        return base.substring(point + 1);
    }
}
