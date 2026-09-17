package tn.cafe.pos.desktop.core.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.application.Platform;
import tn.cafe.pos.desktop.config.AppConfig;
import tn.cafe.pos.desktop.core.i18n.I18n;

/**
 * SSE reader for GET /notifications/stream.
 * Backend events: "connecte" + "nouvelle-commande" (plain text payload).
 * Auto-reconnects with backoff. Callbacks run on FX thread.
 */
public class SseClient {
    private Thread worker;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public void start(Consumer<String> onEvent, Consumer<String> onStatus) {
        stop();
        running.set(true);
        worker = new Thread(() -> {
            int backoff = 2000;
            while (running.get()) {
                try {
                    status(onStatus, I18n.t("sse.connecting"));
                    var req = HttpRequest.newBuilder(URI.create(AppConfig.sseUrl()))
                            .timeout(Duration.ofMinutes(30))
                            .header("Accept", "text/event-stream").GET().build();
                    var res = ApiClient.get().http().send(req, HttpResponse.BodyHandlers.ofInputStream());
                    if (res.statusCode() != 200) throw new IllegalStateException("SSE HTTP " + res.statusCode());
                    status(onStatus, I18n.t("sse.live"));
                    backoff = 2000;
                    String event = null;
                    try (var br = new BufferedReader(new InputStreamReader(res.body(), StandardCharsets.UTF_8))) {
                        String line;
                        while (running.get() && (line = br.readLine()) != null) {
                            if (line.startsWith("event:")) event = line.substring(6).trim();
                            else if (line.startsWith("data:")) {
                                String data = line.substring(5).trim();
                                String e = event == null ? "message" : event;
                                event = null;
                                if (!"connecte".equals(e)) emit(onEvent, e + " :: " + data);
                            }
                        }
                    }
                } catch (Exception ex) {
                    if (!running.get()) break;
                    status(onStatus, I18n.t("sse.reconnecting", ex.getMessage()));
                    try { Thread.sleep(backoff); } catch (InterruptedException ie) { break; }
                    backoff = Math.min(backoff * 2, 15000);
                }
            }
        }, "pos-sse");
        worker.setDaemon(true);
        worker.start();
    }

    public void stop() {
        running.set(false);
        if (worker != null) worker.interrupt();
    }

    private static void emit(Consumer<String> c, String v) { if (c != null) Platform.runLater(() -> c.accept(v)); }
    private static void status(Consumer<String> c, String v) { if (c != null) Platform.runLater(() -> c.accept(v)); }
}
