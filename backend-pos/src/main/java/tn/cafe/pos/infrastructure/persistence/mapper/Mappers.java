package tn.cafe.pos.infrastructure.persistence.mapper;

import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.infrastructure.persistence.document.*;
import java.util.stream.Collectors;

public final class Mappers {
    private Mappers() {}

    public static User toUser(UserDocument d) {
        return new User(d.id, d.username, d.passwordHash, d.pinHash, d.qrKey,
                Role.valueOf(d.role), d.actif, d.creeLe);
    }
    public static UserDocument toUserDoc(User u) {
        UserDocument d = new UserDocument();
        d.id = u.getId(); d.username = u.getUsername(); d.passwordHash = u.getPasswordHash();
        d.pinHash = u.getPinHash(); d.qrKey = u.getQrKey(); d.role = u.getRole().name();
        d.actif = u.isActif(); d.creeLe = u.getCreeLe();
        return d;
    }

    public static Category toCategory(CategoryDocument d) {
        return new Category(d.id, d.nom, d.description, d.ordre, d.active, d.creeLe);
    }
    public static CategoryDocument toCategoryDoc(Category c) {
        CategoryDocument d = new CategoryDocument();
        d.id = c.getId(); d.nom = c.getNom(); d.description = c.getDescription();
        d.ordre = c.getOrdre(); d.active = c.isActive(); d.creeLe = c.getCreeLe();
        return d;
    }

    public static Product toProduct(ProductDocument d) {
        return new Product(d.id, d.nom, d.prix, d.categorieId, d.description, d.imageUrl, d.disponible, d.creeLe);
    }
    public static ProductDocument toProductDoc(Product p) {
        ProductDocument d = new ProductDocument();
        d.id = p.getId(); d.nom = p.getNom(); d.prix = p.getPrix(); d.categorieId = p.getCategorieId();
        d.description = p.getDescription(); d.imageUrl = p.getImageUrl();
        d.disponible = p.isDisponible(); d.creeLe = p.getCreeLe();
        return d;
    }

    public static Order toOrder(OrderDocument d) {
        var items = d.items == null ? java.util.List.<OrderItem>of() :
            d.items.stream().map(i -> new OrderItem(i.produitId, i.nomProduit, i.prixUnitaire, i.quantite))
                .collect(Collectors.toList());
        return new Order(d.id, d.numero, items, d.total,
                d.statut == null ? OrderStatus.EN_ATTENTE : OrderStatus.valueOf(d.statut),
                d.tableOuClient, d.creeLe);
    }
    public static OrderDocument toOrderDoc(Order o) {
        OrderDocument d = new OrderDocument();
        d.id = o.getId(); d.numero = o.getNumero(); d.total = o.getTotal();
        d.statut = o.getStatut().name(); d.tableOuClient = o.getTableOuClient(); d.creeLe = o.getCreeLe();
        d.items = o.getItems().stream().map(i -> {
            OrderDocument.Item it = new OrderDocument.Item();
            it.produitId = i.getProduitId(); it.nomProduit = i.getNomProduit();
            it.prixUnitaire = i.getPrixUnitaire(); it.quantite = i.getQuantite();
            return it;
        }).collect(Collectors.toList());
        return d;
    }

    public static Ticket toTicket(TicketDocument d) {
        return new Ticket(d.id, d.commandeId, d.numeroCommande, TicketType.valueOf(d.type), d.contenu, d.creeLe);
    }
    public static TicketDocument toTicketDoc(Ticket t) {
        TicketDocument d = new TicketDocument();
        d.id = t.getId(); d.commandeId = t.getCommandeId(); d.numeroCommande = t.getNumeroCommande();
        d.type = t.getType().name(); d.contenu = t.getContenu(); d.creeLe = t.getCreeLe();
        return d;
    }

    public static Payment toPayment(PaymentDocument d) {
        return new Payment(d.id, d.commandeId, PaymentType.valueOf(d.type), d.montant,
                PaymentStatus.valueOf(d.statut), d.referenceSimulee, d.creeLe);
    }
    public static PaymentDocument toPaymentDoc(Payment p) {
        PaymentDocument d = new PaymentDocument();
        d.id = p.getId(); d.commandeId = p.getCommandeId(); d.type = p.getType().name();
        d.montant = p.getMontant(); d.statut = p.getStatut().name();
        d.referenceSimulee = p.getReferenceSimulee(); d.creeLe = p.getCreeLe();
        return d;
    }
}
