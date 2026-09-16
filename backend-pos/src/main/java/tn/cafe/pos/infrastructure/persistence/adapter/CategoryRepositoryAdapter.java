package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import tn.cafe.pos.domain.model.Category;
import tn.cafe.pos.domain.repository.CategoryRepository;
import tn.cafe.pos.infrastructure.persistence.mapper.Mappers;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringCategoryMongo;

@Repository
public class CategoryRepositoryAdapter implements CategoryRepository {
    private final SpringCategoryMongo spring;
    public CategoryRepositoryAdapter(SpringCategoryMongo spring) { this.spring = spring; }

    @Override public Category save(Category c) { return Mappers.toCategory(spring.save(Mappers.toCategoryDoc(c))); }
    @Override public Optional<Category> findById(String id) { return spring.findById(id).map(Mappers::toCategory); }
    @Override public Optional<Category> findByNom(String nom) { return spring.findByNom(nom).map(Mappers::toCategory); }
    @Override public List<Category> findAll() { return spring.findAll().stream().map(Mappers::toCategory).toList(); }
    @Override public List<Category> findAllActive() {
        return spring.findByActiveTrueOrderByOrdreAsc().stream().map(Mappers::toCategory).toList();
    }
    @Override public void deleteById(String id) { spring.deleteById(id); }
}
