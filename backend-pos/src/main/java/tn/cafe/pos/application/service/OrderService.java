package tn.cafe.pos.application.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import tn.cafe.pos.application.dto.CreateOrderRequest;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.*;
import tn.cafe.pos.domain.repository.OrderRepository;
import tn.cafe.pos.domain.repository.ProductRepository;
import tn.cafe.pos.infrastructure.realtime.OrderSseHub;

@Service
public class OrderService {
    private final OrderRepository commandes;
    private final ProductRepository produits;
    private final OrderSseHub hub;

    public OrderService(OrderRepository commandes, ProductRepository produits, OrderSseHub hub) {
        this.commandes = commandes; this.produits = produits; this.hub = hub;
    }

    /** Le client crée la commande : prix figés depuis les produits, notif temps réel au gérant. */
    public Order creer(CreateOrderRequest req) {
        List<OrderItem> items = new ArrayList<>();
        for (var li : req.items()) {
            if (li.quantite() < 1) throw new BusinessException("Quantité >= 1 requise");
            Product p = produits.findById(li.produitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable : " + li.produitId()));
            if (!p.isDisponible()) throw new BusinessException("Produit indisponible : " + p.getNom());
            items.add(new OrderItem(p.getId(), p.getNom(), p.getPrix(), li.quantite()));
        }
        Order o = Order.creer(items, req.tableOuClient());
        o.setNumero(genererNumero());
        Order sauvee = commandes.save(o);
        hub.diffuserCreation(sauvee);
        return sauvee;
    }

    public Order parId(String id) {
        return commandes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Commande introuvable"));
    }

    public List<Order> toutes() { return commandes.findAll(); }
    public List<Order> parStatut(OrderStatus s) { return commandes.findByStatut(s); }

    public Order changerStatut(String id, OrderStatus next) {
        Order o = parId(id);
        o.changerStatut(next);
        return commandes.save(o);
    }

    public Order annuler(String id) {
        Order o = parId(id);
        o.annuler();
        return commandes.save(o);
    }

    public Order sauvegarder(Order o) { return commandes.save(o); }

    private String genererNumero() {
        long n = commandes.count() + 1;
        return "CMD-" + String.format("%06d", n);
    }
}
