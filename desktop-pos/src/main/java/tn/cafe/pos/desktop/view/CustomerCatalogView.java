package tn.cafe.pos.desktop.view;

import java.util.ArrayList;
import java.util.List;
import javafx.animation.ScaleTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Category;
import tn.cafe.pos.desktop.model.Product;
import tn.cafe.pos.desktop.viewmodel.CartStore;

/**
 * S1 — customer catalogue: category chips + touch product grid + cart dock
 * (lines, grand-total bar, see-cart action). Falls back gracefully when the
 * backend is unreachable. All labels are RTL-crash-safe with bounded widths.
 */
public class CustomerCatalogView extends BorderPane {
    private final Router router;
    private final FlowPane grid = new FlowPane();
    private final HBox catBar = new HBox(10);
    private final Label cartLabel = new Label();
    private final ListView<String> cartLines = new ListView<>();
    private List<Product> all = new ArrayList<>();
    private String filterCat = null;
    private String search = "";
    private long imageWaitStarted;
    private final java.util.Map<String, VBox> qtyBoxes = new java.util.HashMap<>();
    private final java.util.Map<String, Product> productsById = new java.util.HashMap<>();
    private final StackPane contentLayer = new StackPane();
    private final javafx.scene.control.ProgressIndicator screenLoader = new javafx.scene.control.ProgressIndicator();

    public CustomerCatalogView(Router router) {
        this.router = router;
        getStyleClass().add("root");
        setTop(new VBox(Ui.topBar(router, I18n.t("catalog.title"), true), Ui.motifStrip()));

        var searchField = new TextField();
        searchField.setPromptText(Ui.safe(I18n.t("catalog.search")));
        searchField.getStyleClass().add("search");
        searchField.textProperty().addListener((o, a, b) -> { search = b == null ? "" : b.toLowerCase(); render(); });

        catBar.setPadding(new Insets(10, 16, 4, 16));
        catBar.setAlignment(Pos.CENTER_LEFT);
        catBar.setMinHeight(58);
        var categoryScroll = new ScrollPane(catBar);
        categoryScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        categoryScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        categoryScroll.setFitToHeight(true);
        // Fixed height: content (58) + room for the horizontal bar, never shrinks,
        // so chips can never be clipped or overlapped by the grid below.
        categoryScroll.setMinHeight(78);
        categoryScroll.setPrefHeight(78);
        categoryScroll.setMaxHeight(78);
        VBox.setVgrow(categoryScroll, Priority.NEVER);
        categoryScroll.setPannable(true);
        categoryScroll.getStyleClass().addAll("scroll", "category-scroll");

        grid.setPadding(new Insets(12, 16, 12, 16));
        grid.setHgap(14);
        grid.setVgap(14);
        grid.setAlignment(Pos.TOP_LEFT);
        var scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setMinHeight(120);
        scroll.setPannable(true);
        scroll.getStyleClass().add("scroll");

        var searchRow = new HBox(searchField);
        searchRow.setPadding(new Insets(10, 16, 0, 16));
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setMaxWidth(Double.MAX_VALUE);

        var center = new VBox(8, searchRow, categoryScroll, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        screenLoader.setPrefSize(54, 54);
        screenLoader.setMaxSize(54, 54);
        screenLoader.getStyleClass().add("screen-loader");
        contentLayer.getChildren().addAll(center, screenLoader);
        StackPane.setAlignment(screenLoader, Pos.CENTER);
        setCenter(contentLayer);
        var orderPanel = cartDock();
        setRight(orderPanel);
        widthProperty().addListener((observable, oldWidth, newWidth) -> {
            if (newWidth.doubleValue() < 900) {
                setRight(null);
                orderPanel.setPrefWidth(USE_COMPUTED_SIZE);
                orderPanel.setMaxWidth(Double.MAX_VALUE);
                cartLines.setPrefHeight(120);
                setBottom(orderPanel);
            } else {
                setBottom(null);
                orderPanel.setPrefWidth(300);
                orderPanel.setMaxWidth(300);
                cartLines.setPrefHeight(USE_COMPUTED_SIZE);
                setRight(orderPanel);
            }
        });
        load();
    }

    private VBox cartDock() {
        var dock = new VBox(12);
        dock.getStyleClass().add("cart-dock");
        dock.setAlignment(Pos.TOP_CENTER);
        dock.setPrefWidth(300);
        dock.setMinWidth(0);
        dock.setPadding(new Insets(18, 16, 18, 16));

        var title = Ui.section(I18n.t("cart.items"));
        cartLines.getStyleClass().add("cart-lines");
        cartLines.setMinHeight(120);
        VBox.setVgrow(cartLines, Priority.ALWAYS);

        var totalBar = new HBox(cartLabel);
        totalBar.getStyleClass().add("grand-total");
        totalBar.setAlignment(Pos.CENTER);
        totalBar.setMaxWidth(Double.MAX_VALUE);
        cartLabel.getStyleClass().add("cart-total");
        cartLabel.setWrapText(false);
        cartLabel.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        cartLabel.setMaxWidth(Double.MAX_VALUE);
        cartLabel.setAlignment(Pos.CENTER);
        HBox.setHgrow(cartLabel, Priority.ALWAYS);

        refreshCart();
        var voir = Ui.big(I18n.t("catalog.viewCart"), "primary");
        voir.setOnAction(e -> router.go(Router.Route.CART));
        CartStore.get().lines().addListener((javafx.collections.ListChangeListener<? super CartStore.Line>) c -> {
            refreshCart();
            refreshQuantities();
        });
        dock.getChildren().addAll(title, cartLines, totalBar, voir);
        return dock;
    }

    private void refreshCart() {
        int n = CartStore.get().lines().stream().mapToInt(CartStore.Line::qty).sum();
        cartLabel.setText(Ui.safe(I18n.t("catalog.cart", n, Ui.montant(CartStore.get().total()))));
        cartLines.getItems().clear();
        for (var l : CartStore.get().lines()) {
            cartLines.getItems().add(Ui.safe(l.qty() + " x " + l.product().nom() + " — " + Ui.montant(l.total()) + " TND"));
        }
    }

    private void load() {
        var t = new Task<Void>() {
            List<Category> cats = List.of(); List<Product> prods = List.of();
            @Override protected Void call() {
                try {
                    cats = ApiClient.get().getList("/categories/actives", Category.class);
                } catch (Exception e1) {
                    try { cats = ApiClient.get().getList("/categories", Category.class); }
                    catch (Exception ignored) {}
                }
                try {
                    prods = ApiClient.get().getList("/products?disponibles=true", Product.class);
                } catch (Exception ignored) { prods = List.of(); }
                List<Category> fc = cats; List<Product> fp = prods;
                Platform.runLater(() -> { buildCats(fc); all = new ArrayList<>(fp); render(); });
                return null;
            }
        };
        var thread = new Thread(t, "catalog-load");
        thread.setDaemon(true);
        thread.start();
    }

    private void buildCats(List<Category> cats) {
        catBar.getChildren().clear();
        var tout = catBtn(I18n.t("catalog.all"), null);
        tout.getStyleClass().add("chip-active");
        catBar.getChildren().add(tout);
        for (Category c : cats) catBar.getChildren().add(catBtn(c));
    }

    private Button catBtn(Category category) {
        var button = catBtn(category.nom(), category.id());
        if (category.imageUrl() != null && !category.imageUrl().isBlank()) {
            try {
                var image = new ImageView(new Image(Ui.resolveImageUrl(category.imageUrl()), 28, 28, true, true, true));
                image.setFitWidth(28); image.setFitHeight(28); image.setPreserveRatio(true);
                button.setGraphic(image);
            } catch (RuntimeException ignored) {
                // broken image URL: keep text-only chip
            }
        }
        return button;
    }

    private Button catBtn(String label, String id) {
        var b = new Button(Ui.safe(label));
        b.getStyleClass().addAll("chip", "category-chip");
        b.setMinHeight(44);
        b.setMnemonicParsing(false);
        b.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        b.setOnAction(e -> {
            filterCat = id;
            catBar.getChildren().forEach(n -> n.getStyleClass().remove("chip-active"));
            if (!b.getStyleClass().contains("chip-active")) b.getStyleClass().add("chip-active");
            render();
        });
        return b;
    }

    private void render() {
        grid.getChildren().clear();
        qtyBoxes.clear();
        productsById.clear();
        all.stream()
            .filter(p -> filterCat == null || filterCat.equals(p.categorieId()))
            .filter(p -> search.isBlank() || (p.nom() != null && p.nom().toLowerCase().contains(search)))
            .forEach(p -> grid.getChildren().add(card(p)));
        refreshCart();
        waitForImages();
    }

    private void waitForImages() {
        if (imageWaitStarted == 0) imageWaitStarted = System.nanoTime();
        if (all.isEmpty()) {
            screenLoader.setVisible(false);
            return;
        }
        var check = new PauseTransition(Duration.millis(120));
        check.setOnFinished(e -> {
            boolean ready = grid.lookupAll(".product-image").stream().allMatch(javafx.scene.Node::isVisible);
            if (ready || System.nanoTime() - imageWaitStarted > 5_000_000_000L) {
                screenLoader.setVisible(false);
                imageWaitStarted = 0;
            } else {
                waitForImages();
            }
        });
        check.play();
    }

    private VBox card(Product p) {
        var c = new VBox(8);
        c.getStyleClass().add("product-card");
        c.setPadding(new Insets(12));
        c.setAlignment(Pos.TOP_CENTER);
        c.setMinSize(210, 218);
        c.setMaxWidth(230);
        javafx.scene.Node visual = Ui.webImage(p.imageUrl(), 206, 108, I18n.t("catalog.photo"));
        var nom = Ui.oneLine(p.nom(), "product-name");
        nom.setMaxWidth(206);
        nom.setMinHeight(20);
        nom.setAlignment(Pos.CENTER);
        var prix = Ui.amount(Ui.montant(p.prix()) + " TND", "price");
        prix.setAlignment(Pos.CENTER);
        var qtyBox = new VBox();
        qtyBox.setAlignment(Pos.CENTER);
        qtyBox.setMaxWidth(Double.MAX_VALUE);
        if (p.id() != null) {
            qtyBoxes.put(p.id(), qtyBox);
            productsById.put(p.id(), p);
        }
        refreshQtyBox(p);
        c.getChildren().addAll(visual, nom, prix, qtyBox);
        return c;
    }

    /** Quantity already in the basket drives the card: "Add" when 0, stepper otherwise. */
    private void refreshQtyBox(Product p) {
        if (p.id() == null) return;
        var box = qtyBoxes.get(p.id());
        if (box == null) return;
        box.getChildren().clear();
        int q = CartStore.get().qtyOf(p.id());
        if (q <= 0) {
            var add = new Button(Ui.safe(I18n.t("catalog.add")));
            add.getStyleClass().addAll("btn", "primary");
            add.setMaxWidth(Double.MAX_VALUE);
            add.setMinHeight(48);
            add.setMnemonicParsing(false);
            add.setOnAction(e -> {
                CartStore.get().add(p);
                var st = new ScaleTransition(Duration.millis(150), box);
                st.setFromX(1); st.setToX(1.04); st.setFromY(1); st.setToY(1.04);
                st.setAutoReverse(true); st.setCycleCount(2); st.play();
            });
            box.getChildren().add(add);
        } else {
            var minus = stepperButton("−", Ui.safe(I18n.t("catalog.decrease")));
            minus.setOnAction(e -> CartStore.get().dec(p));
            var qty = Ui.amount(String.valueOf(q), "qty-label");
            qty.setAlignment(Pos.CENTER);
            qty.setMinWidth(44);
            var plus = stepperButton("+", Ui.safe(I18n.t("catalog.increase")));
            plus.setOnAction(e -> CartStore.get().add(p));
            var row = new HBox(8, minus, qty, plus);
            row.setAlignment(Pos.CENTER);
            box.getChildren().add(row);
        }
    }

    private static Button stepperButton(String text, String tooltip) {
        var b = new Button(Ui.safe(text));
        b.getStyleClass().addAll("btn", "stepper");
        b.setMinSize(48, 48);
        b.setPrefSize(48, 48);
        b.setMnemonicParsing(false);
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return b;
    }

    private void refreshQuantities() {
        for (var entry : productsById.entrySet()) refreshQtyBox(entry.getValue());
    }
}
