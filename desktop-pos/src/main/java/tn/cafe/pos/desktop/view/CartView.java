package tn.cafe.pos.desktop.view;

import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Order;
import tn.cafe.pos.desktop.viewmodel.CartStore;

/** S2 — Panier + création commande POST /orders {items:[{produitId,quantite}], tableOuClient}. */
public class CartView extends BorderPane {
    private final Router router;
    private final ListView<CartStore.Line> list = new ListView<>();
    private final Label total = new Label();
    private final TextField table = new TextField();
    private final Label status = new Label();

    public CartView(Router router) {
        this.router = router;
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("cart.title"), true));
        table.setText(CartStore.get().tableOuClientProperty().get());
        table.setPromptText(Ui.safe(I18n.t("cart.table")));
        table.getStyleClass().add("search");
        table.setMaxWidth(Double.MAX_VALUE);
        list.setCellFactory(lv -> new LineCell());
        refresh();
        CartStore.get().lines().addListener(
                (javafx.collections.ListChangeListener<? super CartStore.Line>) c -> refresh());

        var itemsTitle = Ui.section(I18n.t("cart.items"));
        status.setWrapText(false);
        status.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        status.setMaxWidth(Double.MAX_VALUE);
        status.getStyleClass().add("status");
        var center = new VBox(10, itemsTitle, list, table, status);
        center.setPadding(new Insets(16));
        VBox.setVgrow(list, Priority.ALWAYS);
        list.setMinHeight(160);
        total.getStyleClass().add("cart-total");
        total.setWrapText(false);
        total.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        total.setMaxWidth(Double.MAX_VALUE);
        total.setAlignment(Pos.CENTER);
        var totalBar = new HBox(total);
        totalBar.getStyleClass().add("grand-total");
        totalBar.setAlignment(Pos.CENTER);
        totalBar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(total, Priority.ALWAYS);
        var buttons = new HBox(12, orderBtn(), payBtn());
        buttons.setAlignment(Pos.CENTER);
        var bottom = new VBox(10, totalBar, buttons);
        bottom.setAlignment(Pos.CENTER);
        bottom.setPadding(new Insets(12, 16, 16, 16));
        setCenter(Ui.vscroll(center));
        setBottom(bottom);
    }

    private void refresh() {
        list.getItems().setAll(CartStore.get().lines());
        total.setText(Ui.safe(I18n.t("cart.total", Ui.montant(CartStore.get().total()))));
    }

    /** Basket row: name, [-] qty [+], line total, remove. Touch-sized, single-line. */
    private static final class LineCell extends ListCell<CartStore.Line> {
        private final Label name = new Label();
        private final Button minus = new Button("−");
        private final Label qty = new Label();
        private final Button plus = new Button("+");
        private final Label amount = new Label();
        private final Button remove = new Button("×");
        private final HBox row = new HBox(8, name, minus, qty, plus, amount, remove);

        LineCell() {
            row.setAlignment(Pos.CENTER_LEFT);
            name.setWrapText(false);
            name.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
            name.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(name, Priority.ALWAYS);
            for (var b : new Button[]{minus, plus, remove}) {
                b.getStyleClass().addAll("btn", "stepper");
                b.setMinSize(44, 44);
                b.setPrefSize(44, 44);
                b.setMnemonicParsing(false);
            }
            minus.setTooltip(new Tooltip(Ui.safe(I18n.t("catalog.decrease"))));
            plus.setTooltip(new Tooltip(Ui.safe(I18n.t("catalog.increase"))));
            remove.setTooltip(new Tooltip(Ui.safe(I18n.t("cart.remove"))));
            qty.setMinWidth(36);
            qty.setAlignment(Pos.CENTER);
            amount.setMinWidth(90);
            amount.setAlignment(Pos.CENTER_RIGHT);
            Ui.ltr(amount);
        }

        @Override
        protected void updateItem(CartStore.Line line, boolean empty) {
            super.updateItem(line, empty);
            if (empty || line == null) {
                setGraphic(null);
                return;
            }
            name.setText(Ui.safe(line.product().nom()));
            qty.setText(String.valueOf(line.qty()));
            amount.setText(Ui.safe(Ui.montant(line.total()) + " TND"));
            minus.setOnAction(e -> CartStore.get().dec(line.product()));
            plus.setOnAction(e -> CartStore.get().add(line.product()));
            remove.setOnAction(e -> CartStore.get().remove(line.product()));
            setGraphic(row);
        }
    }

    private Button orderBtn() {
        var b = Ui.big(I18n.t("cart.order"), "accent");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setOnAction(e -> commander(false));
        return b;
    }

    private Button payBtn() {
        var b = Ui.big(I18n.t("cart.pay"), "primary");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setOnAction(e -> commander(true));
        return b;
    }

    private void commander(boolean thenPay) {
        if (CartStore.get().isEmpty()) {
            status.setText(I18n.t("cart.empty"));
            Ui.toastError(this, I18n.t("cart.empty"));
            return;
        }
        status.setText(I18n.t("cart.sending"));
        var payload = Map.of("items", CartStore.get().toOrderItems(),
                "tableOuClient", table.getText().isBlank() ? I18n.t("cart.counter") : table.getText());
        CartStore.get().tableOuClientProperty().set(table.getText());
        var t = new Task<Order>() {
            @Override protected Order call() throws Exception {
                return ApiClient.get().post("/orders", payload, Order.class);
            }
            @Override protected void succeeded() {
                Order o = getValue();
                Platform.runLater(() -> {
                    status.setText(I18n.t("cart.created", o.numero()));
                    Ui.toastSuccess(CartView.this, I18n.t("cart.created", o.numero()));
                    if (thenPay) router.go(Router.Route.PAYMENT, o);
                    else { CartStore.get().clear(); router.go(Router.Route.TICKET, o); }
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    status.setText(err);
                    Ui.toastError(CartView.this, err);
                });
            }
        };
        var thread = new Thread(t, "order-create");
        thread.setDaemon(true);
        thread.start();
    }
}
