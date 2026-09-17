package tn.cafe.pos.desktop.view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Category;
import tn.cafe.pos.desktop.model.Product;
import java.util.Map;

/**
 * S9 — manager catalogue: product/category CRUD + availability. JWT writes.
 * No raw IDs to type: categories are picked from a combo; price accepts comma
 * or dot; the web image URL shows a live preview before saving.
 */
public class CatalogAdminView extends BorderPane {
    private final ListView<String> prods = new ListView<>();
    private final ListView<String> cats = new ListView<>();
    private final ComboBox<CatOpt> categoryPicker = new ComboBox<>();
    private final Label status = new Label();
    private final VBox imagePreview = new VBox();
    private java.util.List<Product> loadedProducts = java.util.List.of();
    private java.util.List<Category> loadedCategories = java.util.List.of();

    public CatalogAdminView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("catalogAdmin.title"), true));

        var nom = new TextField(); nom.setPromptText(Ui.safe(I18n.t("catalogAdmin.name")));
        var prix = new TextField(); prix.setPromptText(Ui.safe(I18n.t("catalogAdmin.price")));
        var description = new TextField(); description.setPromptText(Ui.safe(I18n.t("catalogAdmin.description")));
        var imageUrl = new TextField(); imageUrl.setPromptText(Ui.safe(I18n.t("catalogAdmin.imageUrl")));
        for (var f : new TextField[]{nom, prix, description, imageUrl}) {
            f.getStyleClass().add("search");
            f.setMaxWidth(Double.MAX_VALUE);
        }
        categoryPicker.setPromptText(Ui.safe(I18n.t("catalogAdmin.categoryId")));
        categoryPicker.setMaxWidth(Double.MAX_VALUE);
        categoryPicker.setMinHeight(48);

        imagePreview.setAlignment(Pos.CENTER_LEFT);
        imagePreview.getChildren().add(Ui.webImage("", 190, 110, I18n.t("catalog.photo")));
        imageUrl.textProperty().addListener((o, a, b) ->
                imagePreview.getChildren().setAll(Ui.webImage(b, 190, 110, I18n.t("catalog.photo"))));

        var add = Ui.big(I18n.t("catalogAdmin.create"), "primary");
        add.setOnAction(e -> creerProduit(nom, prix, description, imageUrl));
        var catName = new TextField(); catName.setPromptText(Ui.safe(I18n.t("catalogAdmin.categoryName")));
        var catDescription = new TextField(); catDescription.setPromptText(Ui.safe(I18n.t("catalogAdmin.categoryDescription")));
        var catImageUrl = new TextField(); catImageUrl.setPromptText(Ui.safe(I18n.t("catalogAdmin.categoryImageUrl")));
        for (var f : new TextField[]{catName, catDescription, catImageUrl}) {
            f.getStyleClass().add("search");
            f.setMaxWidth(Double.MAX_VALUE);
        }
        var addCategory = Ui.big(I18n.t("catalogAdmin.createCategory"), "accent");
        addCategory.setOnAction(e -> creerCategorie(catName.getText(), catDescription.getText(), catImageUrl.getText()));
        var toggle = new Button(Ui.safe(I18n.t("catalogAdmin.availability"))); toggle.getStyleClass().addAll("btn", "accent");
        toggle.setMinHeight(48);
        toggle.setMnemonicParsing(false);
        toggle.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(toggle, Priority.ALWAYS);
        toggle.setOnAction(e -> toggleDispo());
        var del = new Button(Ui.safe(I18n.t("catalogAdmin.delete"))); del.getStyleClass().addAll("btn", "danger");
        del.setMinHeight(48);
        del.setMnemonicParsing(false);
        del.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(del, Priority.ALWAYS);
        del.setOnAction(e -> supprimer());

        status.setWrapText(false);
        status.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        status.setMaxWidth(Double.MAX_VALUE);
        status.getStyleClass().add("status");
        var form = new VBox(8, Ui.section(I18n.t("catalogAdmin.newProduct")), nom, prix, categoryPicker,
            description, imageUrl, imagePreview, add,
            Ui.section(I18n.t("catalogAdmin.newCategory")), catName, catDescription, catImageUrl, addCategory,
            new HBox(10, toggle, del), status);
        form.setPadding(new Insets(4, 12, 4, 4));
        form.setMinWidth(0);
        var formScroll = Ui.vscroll(form);
        formScroll.setMinHeight(200);

        var split = new HBox(14);
        split.setPadding(new Insets(12, 16, 16, 16));
        var l = new VBox(8, Ui.section(I18n.t("catalogAdmin.products")), prods, formScroll);
        var delCat = new Button(Ui.safe(I18n.t("catalogAdmin.deleteCategory")));
        delCat.getStyleClass().addAll("btn", "danger");
        delCat.setMinHeight(48);
        delCat.setMnemonicParsing(false);
        delCat.setMaxWidth(Double.MAX_VALUE);
        delCat.setOnAction(e -> supprimerCategorie());
        var r = new VBox(8, Ui.section(I18n.t("catalogAdmin.categories")), cats, delCat);
        l.setMinWidth(0); r.setMinWidth(0);
        HBox.setHgrow(l, Priority.ALWAYS); HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(prods, Priority.ALWAYS); VBox.setVgrow(cats, Priority.ALWAYS);
        split.getChildren().addAll(l, r);
        setCenter(split);
        load();
    }

    private void load() {
        load(null);
    }

    private void load(String selectProductId) {
        var t = new Task<Void>() {
            @Override protected Void call() throws Exception {
                try {
                    java.util.List<Product> p = ApiClient.get().getList("/products", Product.class);
                    java.util.List<Category> c = ApiClient.get().getList("/categories", Category.class);
                    Platform.runLater(() -> {
                        loadedProducts = p;
                        loadedCategories = c;
                        String keepCat = categoryPicker.getValue() == null ? null : categoryPicker.getValue().id();
                        categoryPicker.getItems().clear();
                        for (Category x : c) categoryPicker.getItems().add(new CatOpt(x.id(), x.nom()));
                        if (keepCat != null) {
                            for (var opt : categoryPicker.getItems()) {
                                if (opt.id().equals(keepCat)) { categoryPicker.setValue(opt); break; }
                            }
                        }
                        if (categoryPicker.getValue() == null && !categoryPicker.getItems().isEmpty()) {
                            categoryPicker.setValue(categoryPicker.getItems().get(0));
                        }
                        prods.getItems().clear();
                        int select = -1;
                        for (int i = 0; i < p.size(); i++) {
                            Product x = p.get(i);
                            String flag = x.disponible() ? "✓" : "✗";
                            prods.getItems().add(Ui.safe(x.nom() + " • " + x.prix() + " TND • " + flag));
                            if (x.id() != null && x.id().equals(selectProductId)) select = i;
                        }
                        if (select >= 0) { prods.getSelectionModel().select(select); prods.scrollTo(select); }
                        cats.getItems().clear();
                        for (Category x : c) cats.getItems().add(Ui.safe(x.nom()));
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> status.setText(Ui.safe(I18n.t("catalogAdmin.loadFailed", ex.getMessage()))));
                }
                return null;
            }
        };
        var thread = new Thread(t, "catalog-admin-load");
        thread.setDaemon(true);
        thread.start();
    }

    private String selProdId() {
        int index = prods.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= loadedProducts.size()) return null;
        return loadedProducts.get(index).id();
    }

    private void creerProduit(TextField nom, TextField prix, TextField description, TextField imageUrl) {
        String name = nom.getText() == null ? "" : nom.getText().strip();
        if (name.isEmpty()) { status.setText(Ui.safe(I18n.t("catalogAdmin.nameRequired"))); return; }
        var amount = Ui.parseMontant(prix.getText());
        if (amount.isEmpty()) { status.setText(Ui.safe(I18n.t("catalogAdmin.priceInvalid"))); return; }
        CatOpt cat = categoryPicker.getValue();
        if (cat == null) { status.setText(Ui.safe(I18n.t("catalogAdmin.categoryRequired"))); return; }
        String img = imageUrl.getText() == null ? "" : imageUrl.getText().strip();
        if (!Ui.isWebImageUrlOk(img)) { status.setText(Ui.safe(I18n.t("catalogAdmin.imageInvalid"))); return; }
        String desc = description.getText() == null ? "" : description.getText().strip();
        var payload = new java.util.LinkedHashMap<String, Object>();
        payload.put("nom", name);
        payload.put("prix", amount.get());
        payload.put("categorieId", cat.id());
        payload.put("description", desc.isEmpty() ? null : desc);
        payload.put("imageUrl", img.isEmpty() ? null : img);
        var t = new Task<Product>() {
            @Override protected Product call() throws Exception {
                return ApiClient.get().post("/products", payload, Product.class);
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    status.setText(Ui.safe(I18n.t("catalogAdmin.created")));
                    nom.clear(); prix.clear(); description.clear(); imageUrl.clear();
                    load(getValue() == null ? null : getValue().id());
                });
            }
            @Override protected void failed() { Platform.runLater(() -> status.setText(Ui.safe(I18n.t("catalogAdmin.createFailed", getException().getMessage())))); }
        };
        var thread = new Thread(t, "prod-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void creerCategorie(String nom, String description, String imageUrl) {
        String name = nom == null ? "" : nom.strip();
        if (name.isEmpty()) { status.setText(Ui.safe(I18n.t("catalogAdmin.nameRequired"))); return; }
        var t = new Task<Category>() {
            @Override protected Category call() throws Exception {
                return ApiClient.get().post("/categories", Map.of("nom", name, "description", description == null ? "" : description,
                        "ordre", loadedCategories.size(), "imageUrl", imageUrl == null ? "" : imageUrl), Category.class);
            }
            @Override protected void succeeded() { Platform.runLater(() -> { status.setText(Ui.safe(I18n.t("catalogAdmin.categoryCreated"))); load(); }); }
            @Override protected void failed() { Platform.runLater(() -> status.setText(Ui.safe(I18n.t("catalogAdmin.createFailed", getException().getMessage())))); }
        };
        var thread = new Thread(t, "category-create");
        thread.setDaemon(true);
        thread.start();
    }

    private void toggleDispo() {
        int index = prods.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= loadedProducts.size()) return;
        String id = loadedProducts.get(index).id();
        if (id == null) return;
        boolean cur = loadedProducts.get(index).disponible();
        var t = new Task<Product>() {
            @Override protected Product call() throws Exception {
                return ApiClient.get().patch("/products/" + id + "/disponibilite",
                        Map.of("disponible", !cur), Product.class);
            }
            @Override protected void succeeded() { Platform.runLater(() -> load(id)); }
            @Override protected void failed() { Platform.runLater(() -> status.setText(Ui.safe(I18n.t("catalogAdmin.availabilityFailed", getException().getMessage())))); }
        };
        var thread = new Thread(t, "prod-dispo");
        thread.setDaemon(true);
        thread.start();
    }

    private void supprimer() {
        String id = selProdId(); if (id == null) return;
        var t = new Task<Void>() {
            @Override protected Void call() throws Exception { ApiClient.get().delete("/products/" + id); return null; }
            @Override protected void succeeded() { Platform.runLater(() -> load()); }
            @Override protected void failed() { Platform.runLater(() -> status.setText(Ui.safe(I18n.t("catalogAdmin.deleteFailed", getException().getMessage())))); }
        };
        var thread = new Thread(t, "prod-del");
        thread.setDaemon(true);
        thread.start();
    }

    private void supprimerCategorie() {
        int index = cats.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= loadedCategories.size()) return;
        String id = loadedCategories.get(index).id();
        if (id == null) return;
        var t = new Task<Void>() {
            @Override protected Void call() throws Exception { ApiClient.get().delete("/categories/" + id); return null; }
            @Override protected void succeeded() { Platform.runLater(() -> load()); }
            @Override protected void failed() { Platform.runLater(() -> status.setText(Ui.safe(I18n.t("catalogAdmin.deleteFailed", getException().getMessage())))); }
        };
        var thread = new Thread(t, "category-del");
        thread.setDaemon(true);
        thread.start();
    }

    /** Category picker entry: shows the name, carries the id. Single line, never wraps. */
    private record CatOpt(String id, String nom) {
        @Override public String toString() { return nom == null ? "" : nom; }
    }
}
