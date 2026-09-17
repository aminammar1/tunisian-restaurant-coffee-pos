package tn.cafe.pos.desktop.core.theme;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.scene.Scene;

/** Dark / light switch via CSS swap. Persisted in user prefs. */
public class ThemeManager {
    public enum Mode { LIGHT, DARK }
    private static Mode current = Mode.LIGHT;

    static {
        try {
            String saved = java.util.prefs.Preferences.userRoot().node("tn.cafe.pos").get("theme", "LIGHT");
            current = Mode.valueOf(saved);
        } catch (Exception ignored) {}
    }

    public static Mode current() { return current; }

    public static void apply(Scene scene) {
        Application.setUserAgentStylesheet(current == Mode.DARK
                ? new PrimerDark().getUserAgentStylesheet()
                : new PrimerLight().getUserAgentStylesheet());
        scene.getStylesheets().clear();
        String css = current == Mode.DARK ? "/css/dark.css" : "/css/light.css";
        var url = ThemeManager.class.getResource(css);
        if (url != null) scene.getStylesheets().add(url.toExternalForm());
        // base always last-safe: theme files already import base rules
    }

    public static void toggle(Scene scene) {
        current = current == Mode.DARK ? Mode.LIGHT : Mode.DARK;
        try { java.util.prefs.Preferences.userRoot().node("tn.cafe.pos").put("theme", current.name()); }
        catch (Exception ignored) {}
        apply(scene);
    }
}
