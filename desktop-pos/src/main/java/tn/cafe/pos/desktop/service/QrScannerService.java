package tn.cafe.pos.desktop.service;

import com.github.sarxos.webcam.Webcam;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

/**
 * QR scan via webcam-capture + ZXing. Frames -> ImageView, decode loop -> onCode.
 * Si pas de caméra, l'écran propose la saisie manuelle (fallback).
 */
public class QrScannerService {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread worker;
    private volatile Consumer<String> proximityListener;
    private double brightnessBaseline = -1;
    private int dimFrames;
    private long lastProximityAt;

    public Webcam openDefault() {
        try {
            var cams = Webcam.getWebcams();
            if (cams.isEmpty()) return null;
            Webcam cam = Webcam.getDefault();
            cam.setViewSize(new Dimension(640, 480));
            return cam;
        } catch (Exception e) { return null; }
    }

    /**
     * Mean frame brightness 0-255 (subsampled, cheap). Pure function so the
     * proximity simulation stays unit-testable without a camera.
     */
    static double meanBrightness(BufferedImage frame) {
        if (frame == null) return -1;
        int w = frame.getWidth(), h = frame.getHeight();
        if (w <= 0 || h <= 0) return -1;
        long sum = 0;
        int count = 0;
        for (int y = 0; y < h; y += 8) {
            for (int x = 0; x < w; x += 8) {
                int rgb = frame.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
                sum += (r * 30 + g * 59 + b * 11) / 100;
                count++;
            }
        }
        return count == 0 ? -1 : (double) sum / count;
    }

    public void preview(Webcam cam, ImageView view, Consumer<String> onCode, Consumer<String> onStatus) {
        preview(cam, view, onCode, onStatus, null);
    }

    /**
     * Same preview loop plus a small simulation: when an object is held close to
     * the camera the lens goes dark — two consecutive frames below 35% of the
     * running baseline fire {@code onProximity} (at most once per 5 seconds).
     */
    public void preview(Webcam cam, ImageView view, Consumer<String> onCode, Consumer<String> onStatus,
                        Consumer<String> onProximity) {
        stop();
        running.set(true);
        proximityListener = onProximity;
        brightnessBaseline = -1;
        dimFrames = 0;
        lastProximityAt = 0;
        worker = new Thread(() -> {
            try {
                cam.open();
                status(onStatus, "Caméra ouverte — présentez le QR…");
                var reader = new MultiFormatReader();
                while (running.get() && cam.isOpen()) {
                    BufferedImage frame = cam.getImage();
                    if (frame != null) {
                        watchProximity(frame);
                        Image fx = SwingFXUtils.toFXImage(frame, null);
                        Platform.runLater(() -> view.setImage(fx));
                        try {
                            var res = reader.decode(new BinaryBitmap(
                                    new HybridBinarizer(new BufferedImageLuminanceSource(frame))));
                            String code = res.getText();
                            status(onStatus, "QR détecté ✓");
                            emit(onCode, code);
                            break;
                        } catch (Exception notFound) { /* next frame */ }
                    }
                    Thread.sleep(120);
                }
            } catch (Exception ex) {
                status(onStatus, "Caméra indisponible: " + ex.getMessage() + " — saisie manuelle possible.");
            }
        }, "qr-scan");
        worker.setDaemon(true);
        worker.start();
    }

    private void watchProximity(BufferedImage frame) {
        var listener = proximityListener;
        if (listener == null) return;
        double mean = meanBrightness(frame);
        if (mean < 0) return;
        if (brightnessBaseline < 0) { brightnessBaseline = mean; return; }
        brightnessBaseline += (mean - brightnessBaseline) * 0.05;
        if (brightnessBaseline > 1 && mean < brightnessBaseline * 0.35) dimFrames++;
        else dimFrames = 0;
        long now = System.currentTimeMillis();
        if (dimFrames >= 2 && now - lastProximityAt > 5000) {
            lastProximityAt = now;
            dimFrames = 0;
            emit(listener, "close");
        }
    }

    public void stop() {
        running.set(false);
        proximityListener = null;
        brightnessBaseline = -1;
        dimFrames = 0;
        if (worker != null) worker.interrupt();
        try { Webcam.getDefault(); } catch (Exception ignored) {}
        try {
            for (var w : Webcam.getWebcams()) if (w.isOpen()) w.close();
        } catch (Exception ignored) {}
    }

    private static void emit(Consumer<String> c, String v) { if (c != null) Platform.runLater(() -> c.accept(v)); }
    private static void status(Consumer<String> c, String v) { if (c != null) Platform.runLater(() -> c.accept(v)); }
}
