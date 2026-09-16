package tn.cafe.pos.application.service;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.application.dto.CategoryRequest;
import tn.cafe.pos.domain.exception.BusinessException;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.Category;
import tn.cafe.pos.domain.repository.CategoryRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {
    @Mock CategoryRepository repo;
    CategoryService service;

    @BeforeEach void setUp() { service = new CategoryService(repo); }

    @Test
    void creer_ok() {
        when(repo.findByNom("Café")).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        Category c = service.creer(new CategoryRequest("Café", "desc", 1));
        assertEquals("Café", c.getNom());
    }

    @Test
    void creer_doublon() {
        when(repo.findByNom("Café")).thenReturn(Optional.of(Category.creer("Café", null, 0)));
        assertThrows(BusinessException.class, () -> service.creer(new CategoryRequest("Café", null, 0)));
    }

    @Test
    void modifier_ok() {
        Category c = Category.creer("Boissons", null, 0); c.setId("1");
        when(repo.findById("1")).thenReturn(Optional.of(c));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("Sodas", service.modifier("1", new CategoryRequest("Sodas", "frais", 2)).getNom());
    }

    @Test
    void parId_introuvable() {
        when(repo.findById("x")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.parId("x"));
    }

    @Test
    void lister() {
        when(repo.findAll()).thenReturn(List.of(Category.creer("Plats", null, 0)));
        assertEquals(1, service.lister().size());
        verify(repo).findAll();
    }

    @Test
    void supprimer_ok() {
        Category c = Category.creer("Desserts", null, 0); c.setId("9");
        when(repo.findById("9")).thenReturn(Optional.of(c));
        service.supprimer("9");
        verify(repo).deleteById("9");
    }
}
