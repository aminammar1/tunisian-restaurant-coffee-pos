package tn.cafe.pos.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Product(String id, String nom, BigDecimal prix, String categorieId,
                      String description, String imageUrl, boolean disponible, Instant creeLe) {}
