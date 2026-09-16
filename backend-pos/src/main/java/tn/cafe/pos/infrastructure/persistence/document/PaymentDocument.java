package tn.cafe.pos.infrastructure.persistence.document;

import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("paiements")
public class PaymentDocument {
    @Id public String id;
    public String commandeId;
    public String type;
    public BigDecimal montant;
    public String statut;
    public String referenceSimulee;
    public Instant creeLe;
}
