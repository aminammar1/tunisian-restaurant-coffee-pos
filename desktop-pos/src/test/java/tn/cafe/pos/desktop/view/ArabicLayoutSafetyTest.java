package tn.cafe.pos.desktop.view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tn.cafe.pos.desktop.core.i18n.I18n;

/**
 * Regression test for the Arabic layout crash
 * ({@code PrismTextLayout.computeTrailingSpaceWidth} AIOOBE on JavaFX 23/24).
 *
 * <p>Renders every bundled string through every {@link Ui} text factory at a
 * deliberately narrow width (200px, where soft-wrapping used to split complex
 * Arabic runs and crash) and forces CSS + layout. Fails on any exception.
 */
class ArabicLayoutSafetyTest {
    @BeforeAll
    static void initToolkit() throws Exception {
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        var latch = new CountDownLatch(1);
        try { Platform.startup(latch::countDown); } catch (IllegalStateException alreadyRunning) { latch.countDown(); }
        if (!latch.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("FX toolkit did not start");
    }

    @Test
    void everyBundleStringLaysOutAtNarrowWidth() {
        for (I18n.Language lang : I18n.Language.values()) {
            Locale locale = switch (lang) {
                case AR -> Locale.forLanguageTag("ar");
                case FR -> Locale.FRENCH;
                case EN -> Locale.ENGLISH;
            };
            var bundle = java.util.ResourceBundle.getBundle("i18n.messages", locale);
            for (String key : bundle.keySet()) {
                String value = bundle.getString(key);
                layout("title/" + lang + "/" + key, Ui.title(value));
                layout("subtitle/" + lang + "/" + key, Ui.subtitle(value));
                layout("paragraph/" + lang + "/" + key, Ui.paragraph(value, "subtitle"));
                layout("big/" + lang + "/" + key, Ui.big(value, "primary"));
                layout("section/" + lang + "/" + key, Ui.section(value));
            }
        }
    }

    private static void layout(String name, javafx.scene.Node node) {
        assertDoesNotThrow(() -> runOnFx(() -> {
            var box = new VBox(node);
            box.setAlignment(Pos.TOP_LEFT);
            box.setMaxWidth(200);
            box.setPrefWidth(200);
            var root = new StackPane(box);
            var scene = new Scene(root, 1280, 800);
            tn.cafe.pos.desktop.core.theme.ThemeManager.apply(scene);
            root.applyCss();
            root.layout();
        }), name);
    }

    private static void runOnFx(Runnable work) throws Exception {
        var error = new AtomicReference<Throwable>();
        var done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try { work.run(); }
            catch (Throwable t) { error.set(t); }
            finally { done.countDown(); }
        });
        if (!done.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("FX task timed out");
        if (error.get() != null) throw new RuntimeException(error.get());
    }
}
