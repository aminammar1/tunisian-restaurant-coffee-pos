package tn.cafe.pos.desktop.core.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Guards the JavaFX Arabic crash: PrismTextLayout.computeTrailingSpaceWidth throws
 * ArrayIndexOutOfBoundsException on wrapped RTL labels whose lines end with
 * spaces/tabs or whose text ends with blank lines. {@link I18n#sanitize(String)}
 * (applied by every {@code I18n.t} call) must remove those triggers.
 */
class TextSafetyTest {
    @AfterEach
    void restoreLanguage() {
        I18n.set(I18n.Language.EN);
    }

    @Test
    void stripsTrailingWhitespaceOnEveryLine() {
        assertEquals("مرحبا\nعالم", I18n.sanitize("مرحبا   \nعالم\t "));
    }

    @Test
    void dropsTrailingBlankLines() {
        assertEquals("طلب", I18n.sanitize("طلب\n\n"));
    }

    @Test
    void normalisesCrlfAndCollapsesBlankRuns() {
        assertEquals("a\n\nb", I18n.sanitize("a\r\n\r\n\r\nb"));
    }

    @Test
    void nullBecomesEmpty() {
        assertEquals("", I18n.sanitize(null));
    }

    @Test
    void arabicBundleValuesAreLayoutSafe() {
        I18n.set(I18n.Language.AR);
        String[] keys = {"mode.customer.desc", "mode.manager.desc", "ticket.pendingService",
                "ticket.pendingClient", "catalog.cart", "nav.theme", "cart.table"};
        for (String key : keys) {
            String value = I18n.t(key, "5", "12.500");
            for (String line : value.split("\n", -1)) {
                assertEquals(line.stripTrailing(), line, "trailing whitespace in " + key);
            }
            assertFalse(value.endsWith("\n"), "trailing newline in " + key);
        }
    }
}
