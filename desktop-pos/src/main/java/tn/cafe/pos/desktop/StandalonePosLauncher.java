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

/** Starts the bundled local MongoDB and Spring Boot API before opening JavaFX. */
public final class StandalonePosLauncher {
    private static final int MONGO_PORT = 27018;
    private static final int API_PORT = 18080;
    private static final String API_BASE = "http://127.0.0.1:" + API_PORT + "/api/v1";

    private StandalonePosLauncher() {}

    public static void main(String[] args) throws Exception {
        Path appDir = appDirectory();
        Path dataDir = Path.of(System.getProperty("user.home"), ".pos-tunisie", "mongodb-data");
        Path logDir = dataDir.getParent().resolve("logs");
        Files.createDirectories(dataDir);
        Files.createDirectories(logDir);

        Process mongo = startMongo(appDir, dataDir, logDir);
        Process backend = null;
        try {
            waitForPort("127.0.0.1", MONGO_PORT, Duration.ofSeconds(45));
            Path backendJar = findRequired(appDir, "backend-pos-", ".jar");
            String jwtSecret = localSecret();
            backend = startBackend(backendJar, logDir, jwtSecret);
            waitForApi(Duration.ofSeconds(60));
            Process backendProcess = backend;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> stop(backendProcess, mongo), "pos-shutdown"));
            System.setProperty("pos.apiBase", API_BASE);
            DesktopPosApp.main(args);
        } finally {
            stop(backend, mongo);
        }
    }

    private static Process startMongo(Path appDir, Path dataDir, Path logDir) throws IOException {
        Path binary = findRequired(appDir, "mongod", isWindows() ? ".exe" : "");
        if (!isWindows()) binary.toFile().setExecutable(true);
        return new ProcessBuilder(List.of(binary.toString(), "--dbpath", dataDir.toString(),
                "--port", String.valueOf(MONGO_PORT), "--bind_ip", "127.0.0.1",
                "--logpath", logDir.resolve("mongodb.log").toString(), "--logappend", "--quiet"))
                .redirectErrorStream(true).start();
    }

    private static Process startBackend(Path jar, Path logDir, String jwtSecret) throws IOException {
        Path java = Path.of(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
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

    private static void waitForApi(Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(API_BASE + "/categories").toURL().openConnection();
                connection.setConnectTimeout(1000);
                connection.setReadTimeout(1000);
                if (connection.getResponseCode() < 500) return;
            } catch (IOException ignored) {}
            Thread.sleep(250);
        }
        throw new IOException("Local POS API did not start. See ~/.pos-tunisie/logs/backend.log");
    }

    private static void waitForPort(String host, int port, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            try (var socket = new java.net.Socket(host, port)) { return; }
            catch (IOException ignored) { Thread.sleep(250); }
        }
        throw new IOException("Local MongoDB did not start. See ~/.pos-tunisie/logs/mongodb.log");
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

    private static String localSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static boolean isWindows() { return System.getProperty("os.name").startsWith("Windows"); }

    private static void stop(Process... processes) {
        for (Process process : processes) {
            if (process != null && process.isAlive()) process.destroy();
        }
    }
}
