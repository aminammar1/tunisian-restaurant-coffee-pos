package tn.cafe.pos.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.cafe.pos.application.dto.ProductRequest;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.Product;
import tn.cafe.pos.domain.repository.CategoryRepository;
import tn.cafe.pos.domain.repository.ProductRepository;

@Service
public class ProductService {
    private final ProductRepository produits;
    private final CategoryRepository categories;

    public ProductService(ProductRepository produits, CategoryRepository categories) {
        this.produits = produits; this.categories = categories;
    }

    public Product creer(ProductRequest req) {
        categories.findById(req.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        Product p = Product.creer(req.nom(), req.prix(), req.categorieId(), req.description(), req.imageUrl());
        if (req.disponible() != null) p.marquerDisponible(req.disponible());
        return produits.save(p);
    }

    public Product modifier(String id, ProductRequest req) {
        Product p = parId(id);
        categories.findById(req.categorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        p.setNom(req.nom());
        p.setPrix(req.prix());
        p.setCategorieId(req.categorieId());
        p.setDescription(req.description());
        p.setImageUrl(req.imageUrl());
        if (req.disponible() != null) p.marquerDisponible(req.disponible());
        return produits.save(p);
    }

    public Product changerDisponibilite(String id, boolean dispo) {
        Product p = parId(id);
        p.marquerDisponible(dispo);
        return produits.save(p);
    }

    public List<Product> lister() { return produits.findAll(); }
    public List<Product> parCategorie(String catId) { return produits.findByCategorie(catId); }
    public List<Product> disponibles() { return produits.findDisponibles(); }

    public Product parId(String id) {
        return produits.findById(id).orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));
    }

    public void supprimer(String id) {
        parId(id);
        produits.deleteById(id);
    }
}
