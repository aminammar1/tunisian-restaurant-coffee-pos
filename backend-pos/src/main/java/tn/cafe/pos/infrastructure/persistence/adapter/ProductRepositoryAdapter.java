package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import tn.cafe.pos.domain.model.Product;
import tn.cafe.pos.domain.repository.ProductRepository;
import tn.cafe.pos.infrastructure.persistence.mapper.Mappers;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringProductMongo;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {
    private final SpringProductMongo spring;
    public ProductRepositoryAdapter(SpringProductMongo spring) { this.spring = spring; }

    @Override public Product save(Product p) { return Mappers.toProduct(spring.save(Mappers.toProductDoc(p))); }
    @Override public Optional<Product> findById(String id) { return spring.findById(id).map(Mappers::toProduct); }
    @Override public List<Product> findAll() { return spring.findAll().stream().map(Mappers::toProduct).toList(); }
    @Override public List<Product> findByCategorie(String categorieId) {
        return spring.findByCategorieId(categorieId).stream().map(Mappers::toProduct).toList();
    }
    @Override public List<Product> findDisponibles() {
        return spring.findByDisponibleTrue().stream().map(Mappers::toProduct).toList();
    }
    @Override public void deleteById(String id) { spring.deleteById(id); }
}
