package tn.cafe.pos.desktop.view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.ZoneId;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.api.AuthSession;
import tn.cafe.pos.desktop.core.api.SseClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Order;

/**
 * S7 — Dashboard Gérant (FR): commandes temps réel via SSE + liste + changement statut.
 * GET /orders, PATCH /orders/{id}/statut {statut}, POST /orders/{id}/annuler.
 */
public class AdminDashboardView extends BorderPane implements ViewLifecycle {
    private final ListView<String> ordersList = new ListView<>();
    private final ListView<String> feed = new ListView<>();
    private final Label live = new Label(I18n.t("sse.live"));
    private final SseClient sse = new SseClient();
    private Order[] cache = new Order[0];

    public AdminDashboardView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("dashboard.title"), true));

        var head = new VBox(8);
        head.setPadding(new Insets(12, 16, 0, 16));
        var headLine = new HBox(12);
        headLine.setAlignment(Pos.CENTER_LEFT);
        var who = new Label(Ui.safe(I18n.t("dashboard.connected", AuthSession.get().usernameProperty().get())));
        who.getStyleClass().add("subtitle");
        who.setWrapText(false);
        who.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        who.setMaxWidth(420);
        live.getStyleClass().add("live");
        var refresh = new Button(Ui.safe(I18n.t("dashboard.refresh"))); refresh.getStyleClass().add("nav-btn");
        refresh.setMnemonicParsing(false);
        refresh.setOnAction(e -> load());
        var out = new Button(Ui.safe(I18n.t("settings.logout"))); out.getStyleClass().add("nav-btn");
        out.setMnemonicParsing(false);
        out.setOnAction(e -> { sse.stop(); AuthSession.get().clear(); router.go(Router.Route.MODE); });
        headLine.getChildren().addAll(who, live, spacer(), refresh, out);
        var actionsBar = new FlowPane();
        actionsBar.setHgap(8);
        actionsBar.setVgap(8);
        actionsBar.setAlignment(Pos.CENTER_LEFT);
        var catBtn = new Button(Ui.safe(I18n.t("dashboard.catalogue"))); catBtn.getStyleClass().add("chip");
        catBtn.setMnemonicParsing(false);
        catBtn.setOnAction(e -> router.go(Router.Route.CATALOG_ADMIN));
        var tickBtn = new Button(Ui.safe(I18n.t("dashboard.tickets"))); tickBtn.getStyleClass().add("chip");
        tickBtn.setMnemonicParsing(false);
        tickBtn.setOnAction(e -> router.go(Router.Route.TICKETS));
        var setBtn = new Button(Ui.safe(I18n.t("dashboard.settings"))); setBtn.getStyleClass().add("chip");
        setBtn.setMnemonicParsing(false);
        setBtn.setOnAction(e -> router.go(Router.Route.SETTINGS));
        actionsBar.getChildren().addAll(catBtn, tickBtn, setBtn);
        head.getChildren().addAll(headLine, actionsBar);

        ordersList.getStyleClass().add("orders");
        feed.getStyleClass().add("feed");
        feed.getItems().add(I18n.t("dashboard.waiting"));

        var split = new HBox(14);
        split.setPadding(new Insets(12, 16, 16, 16));
        var left = new VBox(8, Ui.section(I18n.t("dashboard.orders")), ordersList, actions());
        var right = new VBox(8, Ui.section(I18n.t("dashboard.live")), feed);
        left.setMinWidth(0); right.setMinWidth(0);
        left.setMaxWidth(Double.MAX_VALUE); right.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(ordersList, Priority.ALWAYS); VBox.setVgrow(feed, Priority.ALWAYS);
        split.getChildren().addAll(left, right);

        var wrap = new VBox(head, split);
        VBox.setVgrow(split, Priority.ALWAYS);
        setCenter(wrap);

        load();
        sse.start(msg -> {
                    feed.getItems().add(0, Ui.safe(msg + "  " + java.time.LocalTime.now(ZoneId.systemDefault()).withNano(0)));
                    load();
                },
                    liveMsg -> live.setText(Ui.safe(liveMsg)));
    }

    private static javafx.scene.layout.Region spacer() {
        var r = new javafx.scene.layout.Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private VBox actions() {
        var cb = new ComboBox<StatusOption>();
        cb.getItems().addAll(
                new StatusOption("CONFIRMEE", I18n.t("dashboard.status.confirmed")),
                new StatusOption("EN_PREPARATION", I18n.t("dashboard.status.preparing")),
                new StatusOption("PRETE", I18n.t("dashboard.status.ready")));
        cb.getSelectionModel().select(1);
        cb.setMinHeight(48);
        cb.setMaxWidth(Double.MAX_VALUE);
        var ok = Ui.big(I18n.t("dashboard.changeStatus"), "primary");
        HBox.setHgrow(ok, Priority.ALWAYS);
        ok.setOnAction(e -> changerStatut(cb.getValue()));
        var cancel = new Button(Ui.safe(I18n.t("dashboard.cancel"))); cancel.getStyleClass().addAll("btn", "danger");
        cancel.setMinHeight(48);
        cancel.setMnemonicParsing(false);
        cancel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(cancel, Priority.ALWAYS);
        cancel.setOnAction(e -> annuler());
        var buttons = new HBox(10, ok, cancel);
        buttons.setAlignment(Pos.CENTER);
        var box = new VBox(10, cb, buttons);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Paid/cancelled orders are terminal: the backend rejects any change with 400. */
    private static boolean isTerminal(Order order) {
        if (order == null || order.statut() == null) return false;
        return "PAYEE".equals(order.statut()) || "ANNULEE".equals(order.statut());
    }

    private void load() {
        var t = new Task<java.util.List<Order>>() {
            @Override protected java.util.List<Order> call() throws Exception { return ApiClient.get().getList("/orders", Order.class); }
            @Override protected void succeeded() {
                cache = getValue().toArray(new Order[0]);
                Platform.runLater(() -> {
                    ordersList.getItems().clear();
                    for (Order o : cache)
                        ordersList.getItems().add(Ui.safe("N°" + o.numero() + " • " + o.statut() + " • " + o.total() + " TND • " + o.tableOuClient()));
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> feed.getItems().add(0, I18n.t("dashboard.loadFailed", Ui.friendlyError(getException()))));
            }
        };
        new Thread(t, "orders-load").start();
    }

    private void changerStatut(StatusOption statut) {
        int index = ordersList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= cache.length || statut == null) return;
        if (isTerminal(cache[index])) {
            feed.getItems().add(0, Ui.safe(I18n.t("dashboard.statusLocked")));
            return;
        }
        String id = cache[index].id();
        var t = new Task<Order>() {
            @Override protected Order call() throws Exception {
                return ApiClient.get().patch("/orders/" + id + "/statut", java.util.Map.of("statut", statut.api()), Order.class);
            }
            @Override protected void succeeded() { Platform.runLater(() -> load()); }
            @Override protected void failed() { Platform.runLater(() -> feed.getItems().add(0, I18n.t("dashboard.statusFailed", Ui.friendlyError(getException())))); }
        };
        new Thread(t, "order-status").start();
    }

    private void annuler() {
        int index = ordersList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= cache.length) return;
        if (isTerminal(cache[index])) {
            feed.getItems().add(0, Ui.safe(I18n.t("dashboard.statusLocked")));
            return;
        }
        String id = cache[index].id();
        var t = new Task<Order>() {
            @Override protected Order call() throws Exception {
                return ApiClient.get().post("/orders/" + id + "/annuler", null, Order.class);
            }
            @Override protected void succeeded() { Platform.runLater(() -> load()); }
            @Override protected void failed() { Platform.runLater(() -> feed.getItems().add(0, I18n.t("dashboard.cancelFailed", Ui.friendlyError(getException())))); }
        };
        new Thread(t, "order-cancel").start();
    }

    @Override public void dispose() { sse.stop(); }

    private record StatusOption(String api, String label) {
        @Override public String toString() { return label; }
    }
}
