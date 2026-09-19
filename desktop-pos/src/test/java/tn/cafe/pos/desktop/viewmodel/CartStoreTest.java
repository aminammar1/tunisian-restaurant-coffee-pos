package tn.cafe.pos.desktop.viewmodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import tn.cafe.pos.desktop.model.Product;

/** State-management tests for the customer basket (javafx.base only, no toolkit). */
class CartStoreTest {
    private static Product produit(String id, String prix) {
        return new Product(id, "Café", new BigDecimal(prix), "c1", null, null, true, null);
    }

    @AfterEach
    void vider() {
        CartStore.get().clear();
    }

    @Test
    void ajout_incremente_la_quantite_du_meme_produit() {
        var cafe = produit("p1", "2.500");
        CartStore.get().add(cafe);
        CartStore.get().add(cafe);
        assertEquals(1, CartStore.get().lines().size());
        assertEquals(2, CartStore.get().qtyOf("p1"));
        assertEquals(new BigDecimal("5.000"), CartStore.get().total());
    }

    @Test
    void stepper_fixe_la_quantite_et_zero_retire_la_ligne() {
        var cafe = produit("p1", "2.500");
        CartStore.get().setQty(cafe, 3);
        assertEquals(3, CartStore.get().qtyOf("p1"));
        CartStore.get().setQty(cafe, 1);
        assertEquals(1, CartStore.get().lines().size());
        CartStore.get().setQty(cafe, 0);
        assertTrue(CartStore.get().isEmpty());
        assertEquals(0, CartStore.get().qtyOf("p1"));
    }

    @Test
    void decrement_sous_un_retire_la_ligne() {
        var cafe = produit("p1", "2.500");
        CartStore.get().add(cafe);
        CartStore.get().dec(cafe);
        assertTrue(CartStore.get().isEmpty());
    }

    @Test
    void payload_commande_reprend_les_quantites_choisies() {
        CartStore.get().setQty(produit("p1", "2.500"), 2);
        CartStore.get().setQty(produit("p2", "4.000"), 3);
        var items = CartStore.get().toOrderItems();
        assertEquals(2, items.size());
        assertEquals("p1", items.get(0).get("produitId"));
        assertEquals(2, items.get(0).get("quantite"));
        assertEquals(3, items.get(1).get("quantite"));
        assertEquals(new BigDecimal("17.000"), CartStore.get().total());
    }
}
