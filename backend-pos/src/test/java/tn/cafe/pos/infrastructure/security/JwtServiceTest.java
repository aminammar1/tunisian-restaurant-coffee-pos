package tn.cafe.pos.infrastructure.security;

import org.junit.jupiter.api.Test;
import tn.cafe.pos.infrastructure.config.AppProperties;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private JwtService service() {
        return new JwtService(new AppProperties(
                new AppProperties.Jwt("jwt-test-secret-0123456789-abcdef-987654", 3600000),
                null, null, null, null));
    }

    @Test
    void generer_et_valider() {
        JwtService j = service();
        String t = j.generer("admin", "GERANT");
        assertTrue(j.valide(t));
        assertEquals("admin", j.extraireUsername(t));
    }

    @Test
    void token_invalide() {
        assertFalse(service().valide("faux.token.ici"));
    }
}
