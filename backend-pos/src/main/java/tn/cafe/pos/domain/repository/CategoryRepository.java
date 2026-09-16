package tn.cafe.pos.domain.repository;

import tn.cafe.pos.domain.model.Category;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository {
    Category save(Category c);
    Optional<Category> findById(String id);
    Optional<Category> findByNom(String nom);
    List<Category> findAll();
    List<Category> findAllActive();
    void deleteById(String id);
}
