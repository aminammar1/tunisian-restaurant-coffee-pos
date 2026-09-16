package tn.cafe.pos.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("produits")
public class ProductDocument {
    @Id public String id;
    public String nom;
    public BigDecimal prix;
    public String categorieId;
    public String description;
    public String imageUrl;
    public boolean disponible;
    public Instant creeLe;
}
