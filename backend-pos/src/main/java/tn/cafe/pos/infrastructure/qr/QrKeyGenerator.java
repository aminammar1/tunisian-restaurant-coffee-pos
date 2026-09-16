package tn.cafe.pos.infrastructure.qr;

import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class QrKeyGenerator {
    public String generer() { return "QR-" + UUID.randomUUID().toString().toUpperCase().replace("-", "").substring(0, 16); }
}
