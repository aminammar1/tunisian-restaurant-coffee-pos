package tn.cafe.pos.infrastructure.persistence.document;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("utilisateurs")
public class UserDocument {
    @Id public String id;
    @Indexed(unique = true) public String username;
    public String passwordHash;
    public String pinHash;
    @Indexed(unique = true) public String qrKey;
    public String role;
    public boolean actif;
    public Instant creeLe;
}
