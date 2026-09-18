package tn.cafe.pos.desktop.view;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ContextMenu;
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
     * Human-readable error text: the backend answers {@code {"erreur":"..."}}
     * and {@link tn.cafe.pos.desktop.core.api.ApiClient.ApiException} would otherwise
     * surface raw JSON like {@code HTTP 400 {"erreur":"commande terminée...}}.
     * Pure function (unit-tested, no toolkit needed).
     */
    public static String friendlyError(Throwable ex) {
        if (ex instanceof tn.cafe.pos.desktop.core.api.ApiClient.ApiException api && api.body != null) {
            String body = api.body.strip();
            int key = body.indexOf("\"erreur\"");
            if (key >= 0) {
                int colon = body.indexOf(':', key);
                int open = colon < 0 ? -1 : body.indexOf('"', colon);
                int close = open < 0 ? -1 : body.indexOf('"', open + 1);
                if (close > open) return safe(body.substring(open + 1, close));
            }
            if (!body.isBlank()) return safe(trimmed(body));
        }
        String raw = ex == null ? null : ex.getMessage();
        String msg = (raw == null || raw.isBlank()) ? (ex == null ? "" : ex.getClass().getSimpleName()) : raw.strip();
        return safe(trimmed(msg));
    }

    private static String trimmed(String s) {
        return s.length() > 160 ? s.substring(0, 160) + "..." : s;
    }

    /**
     * The message an admin (not an IT dev) should see: backend {@code erreur}
     * text as-is, a plain "server unreachable" for connection failures, and the
     * friendly fallback otherwise. Pure function (unit-tested).
     */
    public static String essentialError(Throwable ex) {
        if (ex != null) {
            for (Throwable r = ex; r != null; r = r.getCause()) {
                if (r instanceof java.net.ConnectException
                        || r instanceof java.net.http.HttpConnectTimeoutException
                        || r instanceof java.net.UnknownHostException) {
                    return safe(I18n.t("common.offline"));
                }
                String m = r.getMessage();
                if (m != null && (m.contains("Connection refused") || m.contains("Connection reset")
                        || m.contains("timed out") || m.contains("unreachable"))) {
                    return safe(I18n.t("common.offline"));
                }
            }
        }
        return friendlyError(ex);
    }

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
                if (b.doubleValue() >= 1 && !img.isError()) {
                    view.setVisible(true);
                }
            });
            img.errorProperty().addListener((o, a, b) -> {
                view.setVisible(false);
            });
            if (img.getProgress() >= 1 && !img.isError()) {
                view.setVisible(true);
            }
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
     * Brand/glyph icon (Ikonli font packs) tinted to the active theme: crimson on
     * light cards, bright red on dark cards, white on solid red cards. Single
     * glyph node — no text, so RTL-safe by construction.
     */
    public static org.kordamp.ikonli.javafx.FontIcon icon(org.kordamp.ikonli.Ikon ikon, int size) {
        return icon(ikon, size, ThemeManager.current() == ThemeManager.Mode.DARK ? "#ff8d9a" : "#c8102e");
    }

    /** White glyph for solid red surfaces (manager card, red buttons). */
    public static org.kordamp.ikonli.javafx.FontIcon iconOnRed(org.kordamp.ikonli.Ikon ikon, int size) {
        return icon(ikon, size, "#ffffff");
    }

    private static org.kordamp.ikonli.javafx.FontIcon icon(org.kordamp.ikonli.Ikon ikon, int size, String hex) {
        var icon = new org.kordamp.ikonli.javafx.FontIcon(ikon);
        icon.setIconSize(size);
        icon.setIconColor(javafx.scene.paint.Color.web(hex));
        return icon;
    }

    /**
     * Brand logo (espresso cup on crimson tile) from {@code /img/logo.png}.
     * Fixed size with preserved ratio; a missing resource yields an empty
     * region — never breaks layout.
     */
    public static Node brandLogo(double size) {
        var holder = new javafx.scene.layout.StackPane();
        holder.setMinSize(size, size);
        holder.setPrefSize(size, size);
        holder.setMaxSize(size, size);
        try (var in = Ui.class.getResourceAsStream("/img/logo.png")) {
            if (in == null) return holder;
            var img = new javafx.scene.image.Image(in);
            if (img.isError()) return holder;
            var view = new javafx.scene.image.ImageView(img);
            view.setFitWidth(size);
            view.setFitHeight(size);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            holder.getChildren().add(view);
        } catch (Exception ignored) {
            // holder stays empty
        }
        return holder;
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
        return photoBanner("/img/tunisian-cafe.jpg", width, height);
    }

    public static Node sidebarBanner(double width, double height) {
        return photoBanner("/img/tunisian-cafe-sidebar.jpg", width, height);
    }

    private static Node photoBanner(String resource, double width, double height) {
        var holder = new javafx.scene.layout.StackPane();
        holder.setMinSize(width, height);
        holder.setPrefSize(width, height);
        holder.setMaxSize(width, height);
        holder.getStyleClass().add("banner-frame");
        try (var in = Ui.class.getResourceAsStream(resource)) {
            if (in == null) return holder;
            var img = new javafx.scene.image.Image(in);
            if (img.isError()) return holder;
            var view = new javafx.scene.image.ImageView(img);
            double scale = Math.max(width / img.getWidth(), height / img.getHeight());
            view.setFitWidth(img.getWidth() * scale);
            view.setFitHeight(img.getHeight() * scale);
            view.setPreserveRatio(false);
            view.setSmooth(true);
            var clip = new javafx.scene.shape.Rectangle(width, height);
            clip.setArcWidth(28);
            clip.setArcHeight(28);
            holder.setClip(clip);
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

    public static Button iconButton(org.kordamp.ikonli.Ikon ikon, String tooltip) {
        var b = new Button();
        b.setGraphic(icon(ikon, 17));
        b.getStyleClass().addAll(NAV_BUTTON_CLASS, "icon-btn");
        b.setMinSize(42, 42);
        b.setPrefSize(42, 42);
        b.setMaxSize(42, 42);
        b.setTooltip(new Tooltip(safe(tooltip)));
        b.setAccessibleText(safe(tooltip));
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
        bar.getChildren().add(brandLogo(34));
        t.setWrapText(false);
        t.setTextOverrun(OverrunStyle.ELLIPSIS);
        t.setEllipsisString("...");
        t.setMinWidth(0);
        t.setMaxWidth(460);
        t.setTooltip(new Tooltip(safe(title)));
        HBox.setHgrow(t, Priority.SOMETIMES);
        var spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        var language = iconButton(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.GLOBE,
                I18n.t("nav.language"));
        var languageMenu = new ContextMenu();
        for (I18n.Language choice : I18n.Language.values()) {
            var item = new MenuItem(safe(choice.label()));
            item.setOnAction(e -> { I18n.set(choice); router.refresh(); });
            languageMenu.getItems().add(item);
        }
        language.setOnAction(e -> languageMenu.show(language, javafx.geometry.Side.BOTTOM, 0, 0));
        var theme = iconButton(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.MOON,
                I18n.t("nav.theme"));
        theme.setOnAction(e -> {
            var scene = bar.getScene();
            if (scene != null) {
                ThemeManager.toggle(scene);
                theme.setGraphic(icon(ThemeManager.current() == ThemeManager.Mode.DARK
                        ? org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.SUN
                        : org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.MOON, 17));
            }
        });
        var home = iconButton(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.HOME, I18n.t("nav.home"));
        home.setOnAction(e -> router.go(Router.Route.MODE));
        if (canBack) {
            var back = bar.getChildren().get(0);
            ((Button) back).setGraphic(icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.ARROW_LEFT, 16));
            ((Button) back).setText("");
            ((Button) back).setMinSize(42, 42);
            ((Button) back).setPrefSize(42, 42);
            ((Button) back).setMaxSize(42, 42);
            ((Button) back).setTooltip(new Tooltip(safe(I18n.t("nav.back"))));
        }
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
        showToast(root, msg, "toast", 2.5);
    }

    /** Green success toast, Next.js style. Window-level: survives navigation. */
    public static void toastSuccess(javafx.scene.layout.Pane root, String msg) {
        showToast(root, msg, "toast-success", 2.5);
    }

    /** Red error toast with the essential (non-technical) message. Stays longer. */
    public static void toastError(javafx.scene.layout.Pane root, String msg) {
        showToast(root, msg, "toast-error", 4.0);
    }

    private static final java.util.concurrent.atomic.AtomicInteger ACTIVE_TOASTS = new java.util.concurrent.atomic.AtomicInteger();

    private static void showToast(javafx.scene.layout.Pane root, String msg, String cls, double seconds) {
        String text = safe(msg);
        if (text.isBlank()) return;
        try {
            var scene = root.getScene();
            if (scene != null && scene.getWindow() != null) {
                var win = scene.getWindow();
                var pop = new Popup();
                var iconType = "toast-error".equals(cls)
                    ? org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.EXCLAMATION_CIRCLE
                    : org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CHECK_CIRCLE;
                var mark = icon(iconType, 20);
                mark.setIconColor(javafx.scene.paint.Color.web("toast-error".equals(cls) ? "#b42332" : "#1e8e4d"));
                var label = oneLine(text, cls);
                label.setMaxWidth(430);
                var close = new Button("×");
                close.getStyleClass().add("toast-close");
                var content = new HBox(12, mark, label, close);
                content.getStyleClass().addAll("toast-container", cls);
                HBox.setHgrow(label, Priority.ALWAYS);
                close.setOnAction(e -> pop.hide());
                pop.getContent().add(content);
                pop.setAutoHide(true);
                pop.setAutoFix(true);
                int slot = ACTIVE_TOASTS.getAndIncrement();
                double w = 520;
                double x = win.getX() + Math.max(0, (win.getWidth() - w) / 2);
                double y = win.getY() + Math.max(60, win.getHeight() - 110 - (slot % 3) * 58);
                pop.show(win, x, y);
                var hide = new PauseTransition(Duration.seconds(seconds));
                hide.setOnFinished(e -> { pop.hide(); ACTIVE_TOASTS.decrementAndGet(); });
                hide.play();
                return;
            }
        } catch (RuntimeException ignored) {
            // fall through to inline toast
        }
        var label = oneLine(text, cls);
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
