package tn.cafe.pos.domain.repository;

import tn.cafe.pos.domain.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product p);
    Optional<Product> findById(String id);
    List<Product> findAll();
    List<Product> findByCategorie(String categorieId);
    List<Product> findDisponibles();
    void deleteById(String id);
}
