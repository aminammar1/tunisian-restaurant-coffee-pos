package tn.cafe.pos.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import tn.cafe.pos.application.dto.CategoryRequest;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.Category;
import tn.cafe.pos.domain.repository.CategoryRepository;

@Service
public class CategoryService {
    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) { this.repo = repo; }

    public Category creer(CategoryRequest req) {
        repo.findByNom(req.nom().strip()).ifPresent(c -> {
            throw new BusinessException("Catégorie déjà existante : " + req.nom());
        });
        return repo.save(Category.creer(req.nom(), req.description(), req.ordre()));
    }

    public Category modifier(String id, CategoryRequest req) {
        Category c = repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
        c.renommer(req.nom());
        c.setDescription(req.description());
        c.setOrdre(req.ordre());
        return repo.save(c);
    }

    public List<Category> lister() { return repo.findAll(); }
    public List<Category> listerActives() { return repo.findAllActive(); }

    public Category parId(String id) {
        return repo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
    }

    public void supprimer(String id) {
        parId(id);
        repo.deleteById(id);
    }
}
