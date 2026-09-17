package tn.cafe.pos.desktop.core.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class I18nTest {
    @AfterEach
    void restoreLanguage() {
        I18n.set(I18n.Language.EN);
    }

    @Test
    void loadsArabicBundleAndMarksInterfaceRtl() {
        I18n.set(I18n.Language.AR);

        assertTrue(I18n.isRtl());
        assertEquals("الرئيسية", I18n.t("nav.home"));
        assertEquals("تسجيل الدخول", I18n.t("login.connect"));
    }

    @Test
    void fallsBackWithoutReturningAnEmptyLabelWhenKeyIsMissing() {
        I18n.set(I18n.Language.AR);

        assertFalse(I18n.t("missing.key").isBlank());
        assertEquals("missing.key", I18n.t("missing.key"));
    }
}