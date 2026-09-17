package tn.cafe.pos.desktop.core.i18n;

import java.util.Locale;
import java.util.prefs.Preferences;

/**
 * Small, dependency-free UI translation service.  The selected language is stored per
 * workstation so a cashier does not need to choose it again after every restart.
 */
public final class I18n {
    public enum Language {
        EN("EN", "English", false),
        FR("FR", "Français", false),
        AR("AR", "العربية", true);

        private final String code;
        private final String label;
        private final boolean rtl;

        Language(String code, String label, boolean rtl) {
            this.code = code;
            this.label = label;
            this.rtl = rtl;
        }

        public String code() { return code; }
        public String label() { return label; }
        public boolean rtl() { return rtl; }
        @Override public String toString() { return label; }
    }

    private static final Preferences PREFS = Preferences.userRoot().node("tn.cafe.pos");
    private static volatile Language current = load();

    private I18n() {}

    public static Language current() { return current; }

    public static void set(Language language) {
        current = language == null ? Language.EN : language;
        PREFS.put("language", current.name());
    }

    public static boolean isRtl() { return current.rtl(); }

    public static String t(String key, Object... values) {
        String template;
        try {
            template = bundle(current).getString(key);
        } catch (Exception ignored) {
            try {
                template = bundle(Language.EN).getString(key);
            } catch (Exception missing) {
                return key;
            }
        }
        if (values == null || values.length == 0) return sanitize(template);
        try {
            return sanitize(template.formatted(values));
        } catch (RuntimeException ignored) {
            return sanitize(template);
        }
    }

    /**
     * Makes a UI string safe for the JavaFX text engine.
     *
     * <p>PrismTextLayout (notably on RTL/Arabic wrapped labels) crashes with
     * {@code ArrayIndexOutOfBoundsException} in {@code computeTrailingSpaceWidth} when a
     * laid-out line ends with spaces/tabs or the text ends with blank lines. So: strip
     * trailing whitespace per line, drop trailing blank lines, collapse 3+ blank lines,
     * normalise line endings. Leading whitespace is preserved (ticket alignment, etc.).
     */
    public static String sanitize(String text) {
        if (text == null) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            lines[i] = lines[i].replace('\t', ' ').stripTrailing();
        }
        int end = lines.length;
        while (end > 0 && lines[end - 1].isEmpty()) end--;
        var out = new StringBuilder();
        int blanks = 0;
        for (int i = 0; i < end; i++) {
            if (lines[i].isEmpty()) {
                blanks++;
                if (blanks > 1) continue;
            } else {
                blanks = 0;
            }
            if (out.length() > 0) out.append('\n');
            out.append(lines[i]);
        }
        return out.toString();
    }

    private static java.util.ResourceBundle bundle(Language language) {
        Locale locale = switch (language) {
            case AR -> Locale.forLanguageTag("ar");
            case FR -> Locale.FRENCH;
            case EN -> Locale.ENGLISH;
        };
        return java.util.ResourceBundle.getBundle("i18n.messages", locale);
    }

    private static Language load() {
        try { return Language.valueOf(PREFS.get("language", Language.EN.name())); }
        catch (Exception ignored) { return Language.EN; }
    }
}
