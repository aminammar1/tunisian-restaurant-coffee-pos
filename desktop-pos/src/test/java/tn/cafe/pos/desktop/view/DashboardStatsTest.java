package tn.cafe.pos.desktop.view;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tn.cafe.pos.desktop.core.i18n.I18n;

/** Pure-function tests for dashboard stats + live feed labels (no toolkit needed). */
class DashboardStatsTest {
    @Test
    void topSellers_deterministe_avec_egalites() {
        I18n.set(I18n.Language.EN);
        var quantites = Map.of("Espresso", 5, "Café crème", 5, "Thé", 3, "Jus", 5);
        var chiffres = Map.of("Espresso", new BigDecimal("12.500"), "Café crème", new BigDecimal("15.000"),
                "Thé", new BigDecimal("9.000"), "Jus", new BigDecimal("15.000"));
        // 3-way tie at 5: revenue breaks Espresso last, name breaks Café crème before Jus.
        assertEquals("Café crème (5) • Jus (5) • Espresso (5)",
                AdminDashboardView.formatTopSellers(quantites, chiffres, 3, "None yet"));
    }

    @Test
    void topSellers_limite_et_vide() {
        assertEquals("A (2) • B (1)",
                AdminDashboardView.formatTopSellers(Map.of("A", 2, "B", 1), Map.of(), 5, "None yet"));
        assertEquals("None yet", AdminDashboardView.formatTopSellers(Map.of(), Map.of(), 3, "None yet"));
    }

    @Test
    void feed_creation_ne_parle_jamais_de_paiement() {
        I18n.set(I18n.Language.EN);
        String creation = AdminDashboardView.friendlyFeedEvent(
                "commande-creee :: Commande CMD-000001 reçue - Total 5.000 - EN_ATTENTE (paiement en attente)");
        assertTrue(creation.contains("New order received (payment pending)"), creation);
        assertTrue(!creation.contains("Payment confirmed"), creation);

        String legacyCreation = AdminDashboardView.friendlyFeedEvent(
                "nouvelle-commande :: Nouvelle commande CMD-000001 - Total 5.000 - EN_ATTENTE");
        assertTrue(legacyCreation.contains("New order received (payment pending)"), legacyCreation);
    }

    @Test
    void feed_paiement_annonce_seulement_apres_confirmation() {
        I18n.set(I18n.Language.EN);
        String paiement = AdminDashboardView.friendlyFeedEvent(
                "commande-payee :: Paiement confirmé CMD-000001 - Total 5.000 - PAYEE");
        assertTrue(paiement.startsWith("Payment confirmed"), paiement);

        String legacyPaiement = AdminDashboardView.friendlyFeedEvent(
                "nouvelle-commande :: Nouvelle commande CMD-000001 - Total 5.000 - PAYEE");
        assertTrue(legacyPaiement.startsWith("Payment confirmed"), legacyPaiement);
    }

    @Test
    void montant_toujours_trois_decimales() {
        assertEquals("2.500", Ui.montant(new BigDecimal("2.5")));
        assertEquals("5.000", Ui.montant(new BigDecimal("5.000")));
        assertEquals("0.000", Ui.montant(null));
    }

    @Test
    void images_accepte_chemins_backend_relatifs() {
        assertTrue(Ui.isWebImageUrlOk("/api/v1/images/abc.png"));
        assertTrue(Ui.isWebImageUrlOk("https://example.com/p.jpg"));
        String absolue = Ui.resolveImageUrl("https://example.com/p.jpg");
        assertEquals("https://example.com/p.jpg", absolue);
        String relative = Ui.resolveImageUrl("/api/v1/images/abc.png");
        assertTrue(relative.endsWith("/api/v1/images/abc.png"), relative);
        assertTrue(relative.startsWith("http"), relative);
    }
}
