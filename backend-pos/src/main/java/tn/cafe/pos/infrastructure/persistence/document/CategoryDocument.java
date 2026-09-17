package tn.cafe.pos.infrastructure.persistence.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("categories")
public class CategoryDocument {
    @Id public String id;
    @Indexed(unique = true) public String nom;
    public String description;
    public String imageUrl;
    public int ordre;
    public boolean active;
    public Instant creeLe;
}
