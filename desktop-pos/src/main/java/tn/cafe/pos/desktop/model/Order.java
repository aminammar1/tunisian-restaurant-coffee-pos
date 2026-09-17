package tn.cafe.pos.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Order(String id, String numero, List<OrderItem> items, BigDecimal total,
                    String statut, String tableOuClient, Instant creeLe) {}
