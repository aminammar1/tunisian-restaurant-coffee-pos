package tn.cafe.pos.desktop.config;

/** Central config: backend URL, printer, currency. Auto-detected, overridable via -D or env. */
public final class AppConfig {
    private AppConfig() {}

    private static volatile String cachedApiBase;

    /**
     * Resolution order (like Next.js/Angular dev setups):
     * 1. {@code -Dpos.apiBase=...} (explicit, always wins)
     * 2. env {@code POS_API_BASE}
     * 3. {@code POS_API_BASE} / {@code SERVER_PORT} from a nearby {@code .env}
     *    (desktop-pos/.env, backend-pos/.env — searched relative to cwd)
     * 4. default {@code http://localhost:8080/api/v1}
     * So plain {@code ./mvnw javafx:run} just works locally.
     */
    public static String apiBase() {
        String cached = cachedApiBase;
        if (cached != null) return cached;
        synchronized (AppConfig.class) {
            if (cachedApiBase == null) cachedApiBase = resolveApiBase();
            return cachedApiBase;
        }
    }

    private static String resolveApiBase() {
        String sys = System.getProperty("pos.apiBase");
        if (isUsable(sys)) return stripSlash(sys.trim());

        String env = System.getenv("POS_API_BASE");
        if (isUsable(env)) return stripSlash(env.trim());

        String fromDotEnv = fromNearbyDotEnv();
        if (isUsable(fromDotEnv)) return stripSlash(fromDotEnv.trim());

        return "http://localhost:8080/api/v1";
    }

    private static boolean isUsable(String v) {
        return v != null && !v.isBlank() && !v.contains("${");
    }

    private static String stripSlash(String v) {
        return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
    }

    /** Look for POS_API_BASE (preferred) or SERVER_PORT in nearby .env files. */
    private static String fromNearbyDotEnv() {
        var cwd = java.nio.file.Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        var candidates = new java.util.ArrayList<java.nio.file.Path>();
        candidates.add(cwd.resolve(".env"));
        candidates.add(cwd.resolve("backend-pos/.env"));
        candidates.add(cwd.resolve("desktop-pos/.env"));
        candidates.add(cwd.resolveSibling("backend-pos/.env"));
        String portFallback = null;
        for (var p : candidates) {
            var map = parseDotEnv(p);
            if (map == null) continue;
            String base = map.get("POS_API_BASE");
            if (isUsable(base)) return base.trim();
            if (portFallback == null && isUsable(map.get("SERVER_PORT"))) {
                portFallback = map.get("SERVER_PORT").trim();
            }
        }
        if (portFallback != null) return "http://localhost:" + portFallback + "/api/v1";
        return null;
    }

    /** Tolerant .env parser: skips comments/blanks, strips quotes, keeps values with spaces/&/!. */
    static java.util.Map<String, String> parseDotEnv(java.nio.file.Path file) {
        if (!java.nio.file.Files.isRegularFile(file)) return null;
        var map = new java.util.HashMap<String, String>();
        try {
            for (String raw : java.nio.file.Files.readAllLines(file)) {
                String line = raw.replace("\r", "");
                String t = line.strip();
                if (t.isEmpty() || t.startsWith("#") || !t.contains("=")) continue;
                int eq = t.indexOf('=');
                String key = t.substring(0, eq).strip().replaceAll("\\s+", "");
                String value = t.substring(eq + 1).strip();
                if (value.length() >= 2
                        && ((value.startsWith("\"") && value.endsWith("\""))
                            || (value.startsWith("'") && value.endsWith("'")))) {
                    value = value.substring(1, value.length() - 1);
                }
                if (key.isEmpty()) continue;
                map.put(key, value);
            }
        } catch (Exception ignored) {
            return null;
        }
        return map;
    }

    public static String sseUrl() { return apiBase() + "/notifications/stream"; }
    public static String currency() { return "TND"; }
    public static String printerName() {
        return System.getProperty("pos.printer", System.getenv().getOrDefault("POS_PRINTER", "POS-58mm"));
    }
}
