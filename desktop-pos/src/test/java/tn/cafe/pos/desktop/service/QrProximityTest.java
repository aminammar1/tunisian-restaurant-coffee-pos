package tn.cafe.pos.desktop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;

/** Pure-math tests for the camera object-close simulation (no camera needed). */
class QrProximityTest {
    private static BufferedImage solid(Color color) {
        var img = new BufferedImage(64, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 64, 48);
        g.dispose();
        return img;
    }

    @Test
    void brightRoomReadsHigh() {
        assertTrue(QrScannerService.meanBrightness(solid(Color.WHITE)) > 240);
    }

    @Test
    void coveredLensReadsNearZero() {
        assertTrue(QrScannerService.meanBrightness(solid(Color.BLACK)) < 15);
    }

    @Test
    void nullOrEmptyReadsNegative() {
        assertEquals(-1, QrScannerService.meanBrightness(null));
    }

    @Test
    void coverRatioTriggersSimulationThreshold() {
        double room = QrScannerService.meanBrightness(solid(new Color(140, 140, 140)));
        double covered = QrScannerService.meanBrightness(solid(new Color(10, 10, 10)));
        assertTrue(covered < room * 0.35, "covered lens must read below 35% of room baseline");
    }
}
