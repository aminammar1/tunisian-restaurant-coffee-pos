package tn.cafe.pos.desktop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** Pure URI parsing: identical result on Linux / Windows / macOS. */
class AppConfigOriginTest {
    @Test
    void origine_depuis_base_api() {
        assertEquals("http://localhost:8080", AppConfig.serverOrigin("http://localhost:8080/api/v1"));
        assertEquals("http://localhost:8080", AppConfig.serverOrigin("http://localhost:8080/api/v1/"));
        assertEquals("http://127.0.0.1:18080", AppConfig.serverOrigin("http://127.0.0.1:18080/api/v1"));
        assertEquals("https://pos.example.com", AppConfig.serverOrigin("https://pos.example.com/api/v1"));
    }

    @Test
    void base_sans_prefixe_api_inchangee() {
        assertEquals("http://localhost:9000", AppConfig.serverOrigin("http://localhost:9000"));
        assertEquals("", AppConfig.serverOrigin(null));
        assertEquals("", AppConfig.serverOrigin("  "));
    }
}
