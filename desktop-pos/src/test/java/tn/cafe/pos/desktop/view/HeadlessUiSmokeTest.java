package tn.cafe.pos.desktop.view;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.core.theme.ThemeManager;

/**
 * Headless (Monocle + software Prism) smoke test: builds every route in every
 * language and both themes, then forces CSS + layout — the exact pass where the
 * Arabic {@code PrismTextLayout.computeTrailingSpaceWidth} crash happened.
 * No backend needed: API failures degrade to inline status messages.
 */
class HeadlessUiSmokeTest {
    @BeforeAll
    static void initToolkit() throws Exception {
        System.setProperty("glass.platform", "Monocle");
        System.setProperty("monocle.platform", "Headless");
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");
        var latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        if (!latch.await(60, TimeUnit.SECONDS)) throw new IllegalStateException("FX toolkit did not start");
    }

    @Test
    void everyRouteLaysOutInEveryLanguageAndTheme() throws Exception {
        for (I18n.Language lang : I18n.Language.values()) {
            I18n.set(lang);
            for (var mode : ThemeManager.Mode.values()) {
                setTheme(mode);
                for (Router.Route route : Router.Route.values()) {
                    layout(route, lang, mode);
                }
            }
        }
        I18n.set(I18n.Language.EN);
        setTheme(ThemeManager.Mode.LIGHT);
    }

    private static void setTheme(ThemeManager.Mode mode) throws Exception {
        if (ThemeManager.current() != mode) {
            runOnFx(() -> ThemeManager.toggle(new Scene(new javafx.scene.layout.StackPane())));
        }
    }

    private static void layout(Router.Route route, I18n.Language lang, ThemeManager.Mode mode) {
        assertDoesNotThrow(() -> runOnFx(() -> {
            var router = new Router();
            router.go(route);
            var root = router.root();
            var scene = new Scene(root, 1280, 800);
            ThemeManager.apply(scene);
            root.applyCss();
            root.layout();
            assertNoWrappingText(root, route, lang, mode);
            root.getChildren().stream()
                    .filter(ViewLifecycle.class::isInstance)
                    .map(ViewLifecycle.class::cast)
                    .forEach(ViewLifecycle::dispose);
        }), "route " + route + " lang " + lang + " theme " + mode);
    }

    /**
     * No {@code Labeled} must wrap: soft-wrapping a complex (Arabic) run crashes
     * PrismTextLayout (AIOOBE) on JavaFX 23/24. Multi-line copy uses
     * {@code Ui.paragraph} (one single-line label per hard line) instead.
     */
    private static void assertNoWrappingText(javafx.scene.Node node, Router.Route route,
            I18n.Language lang, ThemeManager.Mode mode) {
        String where = "route " + route + " lang " + lang + " theme " + mode + " node " + node.getClass().getSimpleName();
        if (node instanceof javafx.scene.control.Labeled labeled && labeled.isWrapText()) {
            throw new AssertionError("wrapText must stay off: " + where + " text=" + labeled.getText());
        }
        if (node instanceof javafx.scene.control.TextArea area && area.isWrapText()) {
            throw new AssertionError("TextArea wrapText must stay off: " + where);
        }
        if (node instanceof javafx.scene.Parent parent) {
            for (var child : parent.getChildrenUnmodifiable()) assertNoWrappingText(child, route, lang, mode);
        }
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
