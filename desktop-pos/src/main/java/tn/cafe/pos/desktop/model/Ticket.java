package tn.cafe.pos.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Ticket(String id, String commandeId, String numeroCommande, String type, String contenu, Instant creeLe) {}
