package tn.cafe.pos.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.Product;
import tn.cafe.pos.infrastructure.persistence.document.ProductDocument;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringProductMongo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryAdapterTest {
    @Mock SpringProductMongo spring;
    @InjectMocks ProductRepositoryAdapter adapter;

    private ProductDocument doc() {
        ProductDocument d = new ProductDocument();
        d.id = "p1"; d.nom = "Espresso"; d.prix = new BigDecimal("2.500");
        d.categorieId = "c1"; d.disponible = true;
        return d;
    }

    @Test
    void crud() {
        when(spring.save(any())).thenReturn(doc());
        when(spring.findById("p1")).thenReturn(Optional.of(doc()));
        when(spring.findAll()).thenReturn(List.of(doc()));
        when(spring.findByCategorieId("c1")).thenReturn(List.of(doc()));
        when(spring.findByDisponibleTrue()).thenReturn(List.of(doc()));

        Product p = adapter.save(Product.creer("Espresso", new BigDecimal("2.500"), "c1", null, null));
        assertEquals("p1", p.getId());
        assertTrue(adapter.findById("p1").isPresent());
        assertEquals(1, adapter.findAll().size());
        assertEquals(1, adapter.findByCategorie("c1").size());
        assertEquals(1, adapter.findDisponibles().size());
    }
}
