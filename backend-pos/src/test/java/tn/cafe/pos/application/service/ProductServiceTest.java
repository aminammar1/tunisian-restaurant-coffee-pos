package tn.cafe.pos.application.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.application.dto.ProductRequest;
import tn.cafe.pos.domain.exception.ResourceNotFoundException;
import tn.cafe.pos.domain.model.Category;
import tn.cafe.pos.domain.model.Product;
import tn.cafe.pos.domain.repository.CategoryRepository;
import tn.cafe.pos.domain.repository.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {
    @Mock ProductRepository produits;
    @Mock CategoryRepository categories;
    ProductService service;

    @BeforeEach void setUp() { service = new ProductService(produits, categories); }

    private ProductRequest req() {
        return new ProductRequest("Espresso", new BigDecimal("2.500"), "cat1", "serré", null, true);
    }

    @Test
    void creer_ok() {
        when(categories.findById("cat1")).thenReturn(Optional.of(Category.creer("Café", null, 0)));
        when(produits.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals("Espresso", service.creer(req()).getNom());
    }

    @Test
    void creer_categorie_inconnue() {
        when(categories.findById("cat1")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.creer(req()));
    }

    @Test
    void changerDisponibilite() {
        Product p = Product.creer("Thé", new BigDecimal("2.000"), "cat1", null, null);
        p.setId("p1");
        when(produits.findById("p1")).thenReturn(Optional.of(p));
        when(produits.save(any())).thenAnswer(i -> i.getArgument(0));
        assertFalse(service.changerDisponibilite("p1", false).isDisponible());
    }

    @Test
    void lister_et_disponibles() {
        when(produits.findAll()).thenReturn(List.of());
        when(produits.findDisponibles()).thenReturn(List.of(
                Product.creer("Café", new BigDecimal("1.500"), "c", null, null)));
        assertEquals(0, service.lister().size());
        assertEquals(1, service.disponibles().size());
    }

    @Test
    void supprimer_introuvable() {
        when(produits.findById("z")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.supprimer("z"));
    }
}
