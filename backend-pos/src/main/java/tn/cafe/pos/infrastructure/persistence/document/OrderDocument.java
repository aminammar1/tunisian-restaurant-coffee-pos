package tn.cafe.pos.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("commandes")
public class OrderDocument {
    @Id public String id;
    public String numero;
    public List<Item> items;
    public BigDecimal total;
    public String statut;
    public String tableOuClient;
    public Instant creeLe;

    public static class Item {
        public String produitId;
        public String nomProduit;
        public BigDecimal prixUnitaire;
        public int quantite;
    }
}
