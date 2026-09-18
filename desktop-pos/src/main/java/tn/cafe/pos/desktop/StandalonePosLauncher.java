package tn.cafe.pos.desktop;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;

/** Starts the bundled local MongoDB and Spring Boot API before opening JavaFX.
 *
 * <p>On Windows the jpackage launcher is a GUI app with no console, so this class
 * shows an immediate Swing splash window with startup progress. Without it the app
 * looks like it is "just running in the background" for 15-30s while MongoDB and
 * the API boot, and any failure dies silently. All failures now pop a dialog with
 * the log paths instead.</p>
 */
public final class StandalonePosLauncher {
    private static final int MONGO_PORT = 27018;
    private static final int API_PORT = 18080;
    private static final String API_BASE = "http://127.0.0.1:" + API_PORT + "/api/v1";

    private StandalonePosLauncher() {}

    public static void main(String[] args) throws Exception {
        Splash splash = Splash.show();
        Path logDirHint = Path.of(System.getProperty("user.home"), ".pos-tunisie", "logs");
        Process mongo = null;
        Process backend = null;
        try {
            splash.status("Preparing local data folder...");
            Path appDir = appDirectory();
            Path dataDir = Path.of(System.getProperty("user.home"), ".pos-tunisie", "mongodb-data");
            Path logDir = dataDir.getParent().resolve("logs");
            logDirHint = logDir;
            Files.createDirectories(dataDir);
            Files.createDirectories(logDir);

            splash.status("Starting local database (MongoDB)...");
            mongo = startMongo(appDir, dataDir, logDir);
            waitForPort("127.0.0.1", MONGO_PORT, Duration.ofSeconds(45), mongo, logDir);

            splash.status("Starting local API...");
            Path backendJar = findRequired(appDir, "backend-pos-", ".jar");
            String jwtSecret = localSecret();
            backend = startBackend(backendJar, logDir, jwtSecret);
            waitForApi(Duration.ofSeconds(90), backend, logDir);

            splash.status("Opening POS...");
            Process backendProcess = backend;
            Process mongoProcess = mongo;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(backendProcess, mongoProcess), "pos-shutdown"));
            System.setProperty("pos.apiBase", API_BASE);
            splash.close();
            DesktopPosApp.main(args);
        } catch (Exception failure) {
            splash.fail(logDirHint, failure);
            throw failure;
        } finally {
            stop(backend, mongo);
        }
    }

    private static Process startMongo(Path appDir, Path dataDir, Path logDir) throws IOException {
        Path binary = findRequired(appDir, "mongod", isWindows() ? ".exe" : "");
        if (!isWindows()) binary.toFile().setExecutable(true);
        // Merge stdout+stderr into a file: an unread pipe can block the child and,
        // on Windows, a silent mongod crash (e.g. missing VC++ redist) would otherwise
        // leave zero diagnostics. --logpath already captures mongod logs too.
        return new ProcessBuilder(List.of(binary.toString(), "--dbpath", dataDir.toString(),
                "--port", String.valueOf(MONGO_PORT), "--bind_ip", "127.0.0.1",
                "--logpath", logDir.resolve("mongodb.log").toString(), "--logappend", "--quiet"))
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logDir.resolve("mongod-stdout.log").toFile()))
                .start();
    }

    private static Process startBackend(Path jar, Path logDir, String jwtSecret) throws IOException {
        Path java = resolveJavaBinary();
        return new ProcessBuilder(java.toString(), "-jar", jar.toString(),
                "--server.port=" + API_PORT,
                "--spring.mongodb.uri=mongodb://127.0.0.1:" + MONGO_PORT + "/pos_tunisie",
                "--spring.mongodb.database=pos_tunisie",
                "--app.jwt.secret=" + jwtSecret,
                "--app.admin.qr-key=LOCAL-MANAGER-QR")
                .redirectErrorStream(true)
                .redirectOutput(logDir.resolve("backend.log").toFile())
                .start();
    }

    private static void waitForApi(Duration timeout, Process backend, Path logDir) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (backend != null && !backend.isAlive() && backend.exitValue() != 0) {
                throw new IOException("Local POS API exited early (code " + backend.exitValue()
                        + "). See " + logDir.resolve("backend.log"));
            }
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(API_BASE + "/categories").toURL().openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                if (connection.getResponseCode() < 500) return;
            } catch (IOException ignored) {}
            Thread.sleep(250);
        }
        throw new IOException("Local POS API did not start within " + timeout.toSeconds()
                + "s. See " + logDir.resolve("backend.log"));
    }

    private static void waitForPort(String host, int port, Duration timeout, Process watched, Path logDir) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (watched != null && !watched.isAlive() && watched.exitValue() != 0) {
                throw new IOException("Local MongoDB exited early (code " + watched.exitValue()
                        + "). See " + logDir.resolve("mongodb.log")
                        + " — on a fresh Windows PC install the Microsoft Visual C++ Redistributable if mongod is missing DLLs.");
            }
            try (var socket = new java.net.Socket(host, port)) { return; }
            catch (IOException ignored) { Thread.sleep(250); }
        }
        throw new IOException("Local MongoDB did not start within " + timeout.toSeconds()
                + "s. See " + logDir.resolve("mongodb.log"));
    }

    private static Path appDirectory() throws Exception {
        return Path.of(StandalonePosLauncher.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toAbsolutePath().normalize().getParent();
    }

    private static Path findRequired(Path directory, String prefix, String suffix) throws IOException {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .filter(path -> path.getFileName().toString().endsWith(suffix))
                    .findFirst()
                    .orElseThrow(() -> new IOException("Missing bundled file: " + prefix + suffix));
        }
    }

    /**
     * Locates a {@code java} launcher for the bundled backend. jpackage strips
     * {@code bin/java} from the runtime image it generates, so the bundled
     * {@code java.home} cannot be trusted blindly. Resolution order:
     * <ol>
     *   <li>{@code <java.home>/bin/java} when it exists (dev / full JDK runs),</li>
     *   <li>{@code $JAVA_HOME/bin/java} when set,</li>
     *   <li>{@code java} from the OS {@code PATH}.</li>
     * </ol>
     */
    static Path resolveJavaBinary() throws IOException {
        return resolveJavaBinary(Path.of(System.getProperty("java.home")), System.getenv("JAVA_HOME"));
    }

    static Path resolveJavaBinary(Path javaHome, String javaHomeEnv) throws IOException {
        String binary = isWindows() ? "java.exe" : "java";
        Path bundled = javaHome.resolve("bin").resolve(binary);
        if (Files.isExecutable(bundled)) return bundled;
        if (javaHomeEnv != null && !javaHomeEnv.isBlank()) {
            Path fromEnv = Path.of(javaHomeEnv).resolve("bin").resolve(binary);
            if (Files.isExecutable(fromEnv)) return fromEnv;
        }
        // Fall back to PATH resolution; let the OS report a clear error if absent.
        return Path.of(binary);
    }

    private static String localSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static boolean isWindows() { return System.getProperty("os.name").startsWith("Windows"); }

    /**
     * Immediate startup window so the Windows install never looks like a silent
     * background process. Shown on the Swing EDT before MongoDB boots, updated as
     * each stage starts, closed right before the JavaFX window opens. Headless-safe
     * (tests/servers get a no-op).
     */
    private static final class Splash {
        private final javax.swing.JFrame frame;
        private final javax.swing.JLabel status;

        private Splash(javax.swing.JFrame frame, javax.swing.JLabel status) {
            this.frame = frame;
            this.status = status;
        }

        static Splash show() {
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                return new Splash(null, null);
            }
            javax.swing.JLabel statusLabel = new javax.swing.JLabel("Starting...", javax.swing.SwingConstants.CENTER);
            javax.swing.JFrame frame = new javax.swing.JFrame("POS Tunisie — Starting...");
            // Build on EDT but don't block the launcher thread.
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {
                    frame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
                    var panel = new javax.swing.JPanel(new java.awt.BorderLayout(10, 10));
                    panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(18, 22, 18, 22));
                    var title = new javax.swing.JLabel("POS Tunisie", javax.swing.SwingConstants.CENTER);
                    title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 20f));
                    var sub = new javax.swing.JLabel("Café & Restaurant — starting local services...",
                            javax.swing.SwingConstants.CENTER);
                    var bar = new javax.swing.JProgressBar();
                    bar.setIndeterminate(true);
                    panel.add(title, java.awt.BorderLayout.NORTH);
                    panel.add(sub, java.awt.BorderLayout.CENTER);
                    var bottom = new javax.swing.JPanel(new java.awt.BorderLayout(0, 6));
                    bottom.add(bar, java.awt.BorderLayout.NORTH);
                    bottom.add(statusLabel, java.awt.BorderLayout.SOUTH);
                    panel.add(bottom, java.awt.BorderLayout.SOUTH);
                    frame.setContentPane(panel);
                    frame.setSize(420, 180);
                    frame.setLocationRelativeTo(null);
                    frame.setAlwaysOnTop(true);
                    frame.setVisible(true);
                    frame.toFront();
                });
            } catch (Exception ignored) {
                // Splash is best-effort; startup continues without it.
            }
            return new Splash(frame, statusLabel);
        }

        void status(String text) {
            if (frame == null || status == null) return;
            javax.swing.SwingUtilities.invokeLater(() -> {
                status.setText(text);
                frame.toFront();
            });
        }

        void close() {
            if (frame == null) return;
            javax.swing.SwingUtilities.invokeLater(frame::dispose);
        }

        /** Closes the splash and pops a visible error instead of dying silently. */
        void fail(Path logDir, Exception failure) {
            close();
            String detail = failure.getMessage() == null ? failure.toString() : failure.getMessage();
            String message = "POS Tunisie could not start.\n\n" + detail
                    + "\n\nLogs:\n- " + logDir.resolve("backend.log")
                    + "\n- " + logDir.resolve("mongodb.log");
            System.err.println(message);
            failure.printStackTrace();
            if (java.awt.GraphicsEnvironment.isHeadless()) return;
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> javax.swing.JOptionPane.showMessageDialog(
                        null, message, "POS Tunisie — startup failed",
                        javax.swing.JOptionPane.ERROR_MESSAGE));
            } catch (Exception ignored) {}
        }
    }

    private static void stop(Process... processes) {
        for (Process process : processes) {
            if (process != null && process.isAlive()) process.destroy();
        }
    }
}
