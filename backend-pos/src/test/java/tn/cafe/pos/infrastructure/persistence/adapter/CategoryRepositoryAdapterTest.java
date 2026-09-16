package tn.cafe.pos.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tn.cafe.pos.domain.model.Category;
import tn.cafe.pos.infrastructure.persistence.document.CategoryDocument;
import tn.cafe.pos.infrastructure.persistence.mongo.SpringCategoryMongo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryRepositoryAdapterTest {
    @Mock SpringCategoryMongo spring;
    @InjectMocks CategoryRepositoryAdapter adapter;

    @Test
    void save_et_find() {
        CategoryDocument d = new CategoryDocument();
        d.id = "1"; d.nom = "Café"; d.ordre = 1; d.active = true;
        when(spring.save(any())).thenReturn(d);
        when(spring.findById("1")).thenReturn(Optional.of(d));
        when(spring.findAll()).thenReturn(List.of(d));

        Category c = adapter.save(Category.creer("Café", null, 1));
        assertEquals("Café", c.getNom());
        assertTrue(adapter.findById("1").isPresent());
        assertEquals(1, adapter.findAll().size());
        adapter.deleteById("1");
        verify(spring).deleteById("1");
    }

    @Test
    void findAllActive() {
        when(spring.findByActiveTrueOrderByOrdreAsc()).thenReturn(List.of());
        assertTrue(adapter.findAllActive().isEmpty());
    }
}
