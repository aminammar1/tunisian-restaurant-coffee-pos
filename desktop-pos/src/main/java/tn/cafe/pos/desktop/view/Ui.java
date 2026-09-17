package tn.cafe.pos.desktop.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.util.Duration;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.core.theme.ThemeManager;
import tn.cafe.pos.desktop.core.i18n.I18n;

/**
 * Shared UI helpers: safe (RTL-crash-proof) labels, big touch buttons with ellipsis,
 * responsive top bars, section titles, scroll wrappers and toasts.
 *
 * <p><b>No wrapped Labeled, ever.</b> {@code PrismTextLayout.computeTrailingSpaceWidth}
 * throws {@code ArrayIndexOutOfBoundsException} when soft-wrapping splits a complex
 * (Arabic) run — reproducible on JavaFX 23 and 24 with plain wrapped Arabic labels,
 * independent of alignment, font or shaping engine. Every factory below therefore
 * keeps {@code wrapText} off and uses ellipsis; multi-line copy goes through
 * {@link #paragraph(String, String)}, which renders one single-line label per
 * hard line. Every string is also {@link I18n#sanitize sanitized} (no trailing
 * spaces/blank lines) as a second layer of defence.
 */
public final class Ui {
    private static final String NAV_BUTTON_CLASS = "nav-btn";

    /** Upper bound for centered wrapping labels (titles, subtitles, toasts). */
    public static final double MAX_TEXT_WIDTH = 640;

    private Ui() {}

    /** Null-safe + Prism-safe text. */
    public static String safe(String text) { return I18n.sanitize(text); }

    /**
     * Parses a typed price: trims, accepts comma or dot decimals, ignores grouping
     * spaces ("1 000" → 1000). Empty unless the value is a valid non-negative number.
     * Pure function (unit-tested, no toolkit needed).
     */
    public static java.util.Optional<java.math.BigDecimal> parseMontant(String raw) {
        if (raw == null) return java.util.Optional.empty();
        String s = raw.strip().replace(" ", "").replace(',', '.');
        if (s.isEmpty() || s.equals(".") || s.equals("-") || s.equals("+")) return java.util.Optional.empty();
        try {
            var value = new java.math.BigDecimal(s);
            if (value.compareTo(java.math.BigDecimal.ZERO) < 0) return java.util.Optional.empty();
            return java.util.Optional.of(value);
        } catch (NumberFormatException notANumber) {
            return java.util.Optional.empty();
        }
    }

    /** True for blank or http(s) URLs only — what product/category images accept. */
    public static boolean isWebImageUrlOk(String url) {
        if (url == null || url.isBlank()) return true;
        try {
            var uri = new java.net.URI(url.strip());
            String scheme = uri.getScheme();
            return scheme != null && (scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"));
        } catch (Exception bad) {
            return false;
        }
    }

    /**
     * Product visual from a web URL with graceful fallback: placeholder behind, web
     * image on top that only appears once fully loaded without error. Never throws,
     * never wraps text — safe in RTL layouts.
     */
    public static javafx.scene.layout.StackPane webImage(String url, double width, double height, String placeholderText) {
        var ph = oneLine(placeholderText, "image-placeholder");
        ph.setAlignment(Pos.CENTER);
        ph.setMaxWidth(width);
        var stack = new javafx.scene.layout.StackPane(ph);
        stack.setPrefSize(width, height);
        stack.setMaxSize(width, height);
        stack.setMinSize(width, height);
        if (url == null || url.isBlank()) return stack;
        try {
            var img = new javafx.scene.image.Image(url.strip(), width, height, true, true, true);
            var view = new javafx.scene.image.ImageView(img);
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            view.getStyleClass().add("product-image");
            view.setVisible(false);
            // Placeholder shows only while the web image is absent/loading/failed —
            // otherwise its text bleeds through beside portrait photos.
            ph.visibleProperty().bind(view.visibleProperty().not());
            ph.managedProperty().bind(view.visibleProperty().not());
            img.progressProperty().addListener((o, a, b) -> {
                if (b.doubleValue() >= 1 && !img.isError()) view.setVisible(true);
            });
            img.errorProperty().addListener((o, a, b) -> view.setVisible(false));
            if (img.getProgress() >= 1 && !img.isError()) view.setVisible(true);
            stack.getChildren().add(view);
        } catch (RuntimeException badUrl) {
            // keep placeholder only
        }
        return stack;
    }

    /** Force left-to-right for amounts, codes, ticket previews inside RTL screens. */
    public static <T extends Node> T ltr(T node) {
        node.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        return node;
    }

    /**
     * Thin Tunisian zellige strip (red band, white eight-point stars) shown under
     * top bars. Tiled from {@code /img/zellige-strip.png}; falls back to a solid
     * red strip when the resource is missing — never breaks layout.
     */
    public static javafx.scene.layout.Region motifStrip() {
        var strip = new javafx.scene.layout.Region();
        strip.getStyleClass().add("motif-strip");
        strip.setMinHeight(14);
        strip.setPrefHeight(14);
        strip.setMaxHeight(14);
        strip.setMaxWidth(Double.MAX_VALUE);
        try (var in = Ui.class.getResourceAsStream("/img/zellige-strip.png")) {
            if (in != null) {
                var img = new javafx.scene.image.Image(in);
                if (!img.isError()) {
                    var bg = new javafx.scene.layout.Background(new javafx.scene.layout.BackgroundImage(
                            img,
                            javafx.scene.layout.BackgroundRepeat.REPEAT,
                            javafx.scene.layout.BackgroundRepeat.NO_REPEAT,
                            javafx.scene.layout.BackgroundPosition.CENTER,
                            javafx.scene.layout.BackgroundSize.DEFAULT));
                    strip.setBackground(bg);
                }
            }
        } catch (Exception ignored) {
            // fallback red band from CSS
        }
        return strip;
    }

    /**
     * Traditional-Tunis-café photo banner with rounded corners. Fixed size, so a
     * missing resource only leaves a red panel — layout never shifts.
     */
    public static Node tunisiaBanner(double width, double height) {
        var holder = new javafx.scene.layout.StackPane();
        holder.setMinSize(width, height);
        holder.setPrefSize(width, height);
        holder.setMaxSize(width, height);
        holder.getStyleClass().add("banner-frame");
        try (var in = Ui.class.getResourceAsStream("/img/tunisian-cafe.jpg")) {
            if (in == null) return holder;
            var img = new javafx.scene.image.Image(in);
            if (img.isError()) return holder;
            var view = new javafx.scene.image.ImageView(img);
            view.setFitWidth(width);
            view.setFitHeight(height);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            var clip = new javafx.scene.shape.Rectangle(width, height);
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            view.setClip(clip);
            holder.getChildren().add(view);
        } catch (Exception ignored) {
            // red frame fallback from CSS
        }
        return holder;
    }

    public static Button big(String text, String cls) {
        var b = new Button(safe(text));
        b.getStyleClass().addAll("btn", cls);
        b.setMinHeight(52);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setWrapText(false);
        b.setMnemonicParsing(false);
        b.setTextOverrun(OverrunStyle.ELLIPSIS);
        b.setEllipsisString("...");
        b.setTooltip(new Tooltip(safe(text)));
        return b;
    }

    /** Single-line ellipsis label (prices, counters, list-like headers). */
    public static Label oneLine(String text, String styleClass) {
        var l = new Label(safe(text));
        if (styleClass != null && !styleClass.isBlank()) l.getStyleClass().add(styleClass);
        l.setWrapText(false);
        l.setTextOverrun(OverrunStyle.ELLIPSIS);
        l.setEllipsisString("...");
        l.setMaxWidth(Double.MAX_VALUE);
        l.setTooltip(new Tooltip(safe(text)));
        return l;
    }

    /** Amounts / codes: single line, ellipsis, always LTR so digits stay ordered. */
    public static Label amount(String text, String styleClass) {
        var l = oneLine(text, styleClass);
        l.setNodeOrientation(NodeOrientation.LEFT_TO_RIGHT);
        return l;
    }

    public static Label title(String t) {
        var l = oneLine(t, "title");
        l.setAlignment(Pos.CENTER);
        return l;
    }

    public static Label subtitle(String t) {
        var l = oneLine(t, "subtitle");
        l.setAlignment(Pos.CENTER);
        return l;
    }

    /**
     * Multi-line copy without wrapping: one single-line ellipsis label per hard
     * line (split on {@code \n}). Never feeds a soft-wrap split to the text engine.
     */
    public static VBox paragraph(String text, String styleClass) {
        var box = new VBox(2);
        box.setAlignment(Pos.CENTER);
        String safe = safe(text);
        String[] lines = safe.isEmpty() ? new String[]{""} : safe.split("\n", -1);
        for (String line : lines) {
            var l = oneLine(line, styleClass);
            l.setAlignment(Pos.CENTER);
            l.setMaxWidth(MAX_TEXT_WIDTH);
            box.getChildren().add(l);
        }
        return box;
    }

    /** Left-aligned section header for panels and forms. */
    public static Label section(String t) {
        var l = oneLine(t, "section-label");
        l.setAlignment(Pos.CENTER_LEFT);
        return l;
    }

    /** Top bar with back, ellipsis title, theme toggle, language, home. Never overflows. */
    public static HBox topBar(Router router, String title, boolean canBack) {
        var bar = new HBox(10);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(10, 14, 10, 14));
        bar.setMinHeight(64);
        if (canBack) {
            var back = new Button(safe(I18n.t("nav.back")));
            back.getStyleClass().add(NAV_BUTTON_CLASS);
            back.setMnemonicParsing(false);
            back.setTextOverrun(OverrunStyle.ELLIPSIS);
            back.setOnAction(e -> router.back());
            bar.getChildren().add(back);
        }
        var t = new Label(safe(title));
        t.getStyleClass().add("topbar-title");
        t.setWrapText(false);
        t.setTextOverrun(OverrunStyle.ELLIPSIS);
        t.setEllipsisString("...");
        t.setMinWidth(0);
        t.setMaxWidth(460);
        t.setTooltip(new Tooltip(safe(title)));
        HBox.setHgrow(t, Priority.SOMETIMES);
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var language = new MenuButton(safe(I18n.current().code()));
        language.getStyleClass().add("language-menu");
        language.setTextOverrun(OverrunStyle.ELLIPSIS);
        for (I18n.Language choice : I18n.Language.values()) {
            var item = new MenuItem(safe(choice.label()));
            item.setOnAction(e -> { I18n.set(choice); router.refresh(); });
            language.getItems().add(item);
        }
        var theme = new Button(safe(I18n.t("nav.theme")));
        theme.getStyleClass().add(NAV_BUTTON_CLASS);
        theme.setMnemonicParsing(false);
        theme.setTextOverrun(OverrunStyle.ELLIPSIS);
        theme.setOnAction(e -> {
            var scene = bar.getScene();
            if (scene != null) { ThemeManager.toggle(scene); theme.setText(safe(I18n.t("nav.theme"))); }
        });
        var home = new Button(safe(I18n.t("nav.home")));
        home.getStyleClass().add(NAV_BUTTON_CLASS);
        home.setMnemonicParsing(false);
        home.setTextOverrun(OverrunStyle.ELLIPSIS);
        home.setOnAction(e -> router.go(Router.Route.MODE));
        bar.getChildren().addAll(t, spacer, language, theme, home);
        return bar;
    }

    /** Vertical scroll wrapper that never fights inner ListViews (they keep own scroll). */
    public static ScrollPane vscroll(Node content) {
        var scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setPannable(true);
        scroll.getStyleClass().add("scroll");
        return scroll;
    }

    public static void toast(javafx.scene.layout.Pane root, String msg) {
        var text = safe(msg);
        if (text.isBlank()) return;
        var label = new Label(text);
        label.getStyleClass().add("toast");
        label.setWrapText(false);
        label.setTextOverrun(OverrunStyle.ELLIPSIS);
        label.setMaxWidth(480);
        try {
            var scene = root.getScene();
            if (scene != null && scene.getWindow() != null) {
                var pop = new Popup();
                pop.getContent().add(label);
                pop.setAutoHide(true);
                pop.setAutoFix(true);
                pop.show(scene.getWindow());
                var hide = new PauseTransition(Duration.seconds(2.5));
                hide.setOnFinished(e -> pop.hide());
                hide.play();
                return;
            }
        } catch (RuntimeException ignored) {
            // fall through to inline toast
        }
        root.getChildren().add(label);
        StackPanePos.bottom(label, root);
        var ft = new javafx.animation.FadeTransition(javafx.util.Duration.seconds(3), label);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> root.getChildren().remove(label));
        ft.play();
    }

    /** Position helper without extra imports at call sites. */
    private static class StackPanePos {
        static void bottom(Label l, javafx.scene.layout.Pane root) {
            if (root instanceof javafx.scene.layout.StackPane) {
                javafx.scene.layout.StackPane.setAlignment(l, Pos.BOTTOM_CENTER);
                javafx.scene.layout.StackPane.setMargin(l, new Insets(0, 0, 24, 0));
            }
        }
    }
}
