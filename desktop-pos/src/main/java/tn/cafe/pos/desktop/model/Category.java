package tn.cafe.pos.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Category(String id, String nom, String description, int ordre, boolean active, String imageUrl, Instant creeLe) {}
