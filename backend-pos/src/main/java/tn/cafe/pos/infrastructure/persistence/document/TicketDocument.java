package tn.cafe.pos.infrastructure.persistence.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("tickets")
public class TicketDocument {
    @Id public String id;
    public String commandeId;
    public String numeroCommande;
    public String type;
    public String contenu;
    public Instant creeLe;
}
