package tn.cafe.pos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Admin admin,
        Printer printer,
        Ticket ticket,
        Payment payment,
        Images images) {
    public record Jwt(String secret, long expirationMs) {}
    public record Admin(String username, String password, String pin, String qrKey) {}
    public record Printer(boolean enabled, String name) {}
    public record Ticket(int widthMm, String currency, String locale, String footer) {}
    public record Payment(boolean simulationMode, boolean proximityAutoConfirm) {}
    public record Images(String dir, long maxOctets) {}
}
