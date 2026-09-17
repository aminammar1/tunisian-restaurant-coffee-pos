package tn.cafe.pos.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Pure-function tests for admin-form parsing (no toolkit needed). */
class UiParseTest {
    @Test
    void parsesDotAndCommaDecimals() {
        assertEquals(Optional.of(new BigDecimal("4.500")), Ui.parseMontant("4.500"));
        assertEquals(Optional.of(new BigDecimal("4.500")), Ui.parseMontant("4,500"));
        assertEquals(Optional.of(new BigDecimal("12")), Ui.parseMontant("  12  "));
        assertEquals(Optional.of(new BigDecimal("1000")), Ui.parseMontant("1 000"));
        assertEquals(Optional.of(new BigDecimal("0")), Ui.parseMontant("0"));
    }

    @Test
    void rejectsBadPrices() {
        assertEquals(Optional.empty(), Ui.parseMontant(null));
        assertEquals(Optional.empty(), Ui.parseMontant(""));
        assertEquals(Optional.empty(), Ui.parseMontant("   "));
        assertEquals(Optional.empty(), Ui.parseMontant("abc"));
        assertEquals(Optional.empty(), Ui.parseMontant("4.5.0"));
        assertEquals(Optional.empty(), Ui.parseMontant("-5"));
    }

    @Test
    void extractsBackendErreurField() {
        var api = new tn.cafe.pos.desktop.core.api.ApiClient.ApiException(
                400, "{\"erreur\":\"commande terminée : statut non modifiable\"}");
        assertEquals("commande terminée : statut non modifiable", Ui.friendlyError(api));
    }

    @Test
    void fallsBackToPlainMessages() {
        assertEquals("boom", Ui.friendlyError(new RuntimeException("boom")));
        assertEquals("RuntimeException", Ui.friendlyError(new RuntimeException()));
    }

    @Test
    void essentialErrorMapsOffline() {
        tn.cafe.pos.desktop.core.i18n.I18n.set(tn.cafe.pos.desktop.core.i18n.I18n.Language.EN);
        assertEquals("Server unreachable. Start the backend and retry.",
                Ui.essentialError(new java.net.ConnectException("Connection refused")));
        assertEquals("Server unreachable. Start the backend and retry.",
                Ui.essentialError(new RuntimeException("x", new java.net.UnknownHostException("api"))));
        assertEquals("commande terminée", Ui.essentialError(
                new tn.cafe.pos.desktop.core.api.ApiClient.ApiException(400, "{\"erreur\":\"commande terminée\"}")));
        tn.cafe.pos.desktop.core.i18n.I18n.set(tn.cafe.pos.desktop.core.i18n.I18n.Language.EN);
    }

    @Test
    void acceptsBlankOrHttpUrlsOnly() {
        assertTrue(Ui.isWebImageUrlOk(null));
        assertTrue(Ui.isWebImageUrlOk(""));
        assertTrue(Ui.isWebImageUrlOk("https://example.com/p.jpg"));
        assertTrue(Ui.isWebImageUrlOk("http://localhost:8080/x.png"));
        assertFalse(Ui.isWebImageUrlOk("not a url"));
        assertFalse(Ui.isWebImageUrlOk("ftp://example.com/x.jpg"));
    }
}
