package tn.cafe.pos.presentation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tn.cafe.pos.infrastructure.storage.ImageStorageService;
import tn.cafe.pos.presentation.advice.GlobalExceptionHandler;

/** Contract multipart utilisé par le client JavaFX (FileChooser -> POST /images). */
@ExtendWith(MockitoExtension.class)
class ImageControllerTest {
    @Mock private ImageStorageService stockage;
    @TempDir Path tmp;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ImageController(stockage))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void importer_renvoie_201_avec_url_relative() throws Exception {
        when(stockage.stocker(any())).thenReturn("uuid-1.png");

        mvc.perform(multipart("/api/v1/images")
                        .file(new MockMultipartFile("fichier", "cafe.png", "image/png", new byte[]{1, 2})))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nom").value("uuid-1.png"))
                .andExpect(jsonPath("$.url").value("/api/v1/images/uuid-1.png"));
    }

    @Test
    void lire_sert_le_fichier_stocke() throws Exception {
        Path photo = tmp.resolve("uuid-1.png");
        Files.write(photo, new byte[]{1, 2, 3});
        when(stockage.charger("uuid-1.png")).thenReturn(photo);
        when(stockage.typeContenu("uuid-1.png")).thenReturn("image/png");

        mvc.perform(get("/api/v1/images/uuid-1.png"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().string("Content-Type", "image/png"));
    }
}
