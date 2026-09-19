package tn.cafe.pos.desktop.view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.time.ZoneId;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
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
    private static final String STAT_STYLE = "dashboard-stat";
    private static final ZoneId LOCAL_ZONE = ZoneId.systemDefault();
    private final ListView<String> ordersList = new ListView<>();
    private final ListView<String> feed = new ListView<>();
    private final Label live = new Label(I18n.t("sse.live"));
    private final SseClient sse = new SseClient();
    private Order[] cache = new Order[0];
    private java.util.List<Order> visibleOrders = java.util.List.of();
    private final DatePicker day = new DatePicker(LocalDate.now(LOCAL_ZONE));
    private final Label revenue = Ui.oneLine("", STAT_STYLE);
    private final Label orderCount = Ui.oneLine("", STAT_STYLE);
    private final Label bestSeller = Ui.oneLine("", STAT_STYLE);

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
        var today = Ui.big(I18n.t("dashboard.today"), "accent");
        today.setMinHeight(42);
        today.setMaxWidth(150);
        today.setOnAction(e -> { day.setValue(LocalDate.now(LOCAL_ZONE)); refreshOrders(); });
        var out = new Button(Ui.safe(I18n.t("settings.logout"))); out.getStyleClass().add("nav-btn");
        out.setMnemonicParsing(false);
        out.setOnAction(e -> { sse.stop(); AuthSession.get().clear(); router.go(Router.Route.MODE); });
        headLine.getChildren().addAll(who, live, spacer(), day, today, refresh, out);
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

        day.setOnAction(e -> refreshOrders());
        var stats = new HBox(12, statBox(I18n.t("dashboard.revenue"), revenue),
            statBox(I18n.t("dashboard.orderCount"), orderCount),
            statBox(I18n.t("dashboard.bestSeller"), bestSeller));
        stats.getStyleClass().add("dashboard-stats");
        stats.setPadding(new Insets(4, 0, 0, 0));
        head.getChildren().add(stats);

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
                    feed.getItems().add(0, Ui.safe(friendlyFeedEvent(msg) + "  " + java.time.LocalTime.now(LOCAL_ZONE).withNano(0)));
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
                Platform.runLater(AdminDashboardView.this::refreshOrders);
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    feed.getItems().add(0, err);
                    Ui.toastError(AdminDashboardView.this, err);
                });
            }
        };
        var thread = new Thread(t, "orders-load");
        thread.setDaemon(true);
        thread.start();
    }

        private VBox statBox(String title, Label value) {
        var box = new VBox(3, Ui.oneLine(title, "stat-title"), value);
        box.getStyleClass().add("dashboard-stat-box");
        box.setMinWidth(170);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
        }

        private void refreshOrders() {
        LocalDate selected = day.getValue() == null ? LocalDate.now(LOCAL_ZONE) : day.getValue();
        var selectedOrders = java.util.Arrays.stream(cache)
                .filter(o -> o.creeLe() != null && o.creeLe().atZone(LOCAL_ZONE).toLocalDate().equals(selected))
            .toList();
        visibleOrders = selectedOrders;
        ordersList.getItems().setAll(selectedOrders.stream()
            .map(o -> Ui.safe("N°" + o.numero() + " • " + o.statut() + " • " + Ui.montant(o.total()) + " TND • " + o.tableOuClient()))
            .toList());
        // Revenue counts PAID orders only: an order that is merely created must
        // never inflate the takings before its (simulated) payment is confirmed.
        var paid = selectedOrders.stream().filter(o -> "PAYEE".equals(o.statut())).toList();
        BigDecimal total = paid.stream().map(Order::total).filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Integer> quantities = paid.stream().flatMap(o -> o.items().stream())
            .collect(Collectors.groupingBy(i -> i.nomProduit(), Collectors.summingInt(i -> i.quantite())));
        Map<String, BigDecimal> chiffreParProduit = paid.stream().flatMap(o -> o.items().stream())
            .collect(Collectors.groupingBy(i -> i.nomProduit(),
                Collectors.mapping(i -> i.sousTotal() == null ? BigDecimal.ZERO : i.sousTotal(),
                    Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        String best = formatTopSellers(quantities, chiffreParProduit, 3, I18n.t("dashboard.none"));
        revenue.setText(Ui.safe(Ui.montant(total) + " TND"));
        orderCount.setText(Ui.safe(I18n.t("dashboard.orderCountValue", selectedOrders.size(), paid.size())));
        bestSeller.setText(Ui.safe(best));
        }

    /**
     * Top sellers, deterministic even with ties: quantity desc, then product
     * revenue desc, then name asc. Returns "Nom (qté)" joined with " • ".
     * Pure function (unit-tested, no toolkit needed).
     */
    static String formatTopSellers(Map<String, Integer> quantites, Map<String, BigDecimal> chiffres,
                                   int limit, String noneLabel) {
        if (quantites == null || quantites.isEmpty() || limit <= 0) return noneLabel;
        record Entree(String nom, int qte, BigDecimal chiffre) {}
        var entrees = quantites.entrySet().stream()
                .map(e -> new Entree(e.getKey(), e.getValue(),
                        chiffres == null || chiffres.get(e.getKey()) == null
                                ? BigDecimal.ZERO : chiffres.get(e.getKey())))
                .sorted(Comparator.comparingInt(Entree::qte).reversed()
                        .thenComparing(Entree::chiffre, Comparator.reverseOrder())
                        .thenComparing(e -> e.nom() == null ? "" : e.nom(), String.CASE_INSENSITIVE_ORDER))
                .limit(limit)
                .map(e -> (e.nom() == null || e.nom().isBlank() ? "?" : e.nom()) + " (" + e.qte() + ")")
                .toList();
        return entrees.isEmpty() ? noneLabel : String.join(" • ", entrees);
    }

    /**
     * Friendly live-feed line: the backend now sends "commande-creee" (payment
     * still pending) vs "commande-payee"; legacy "nouvelle-commande" payloads
     * are classified by their status word. Pure function (unit-tested).
     */
    static String friendlyFeedEvent(String raw) {
        String event = raw == null ? "" : raw;
        String data = "";
        int sep = event.indexOf(" :: ");
        if (sep >= 0) {
            data = event.substring(sep + 4);
            event = event.substring(0, sep);
        }
        String label;
        if (event.contains("commande-payee")
                || (event.contains("nouvelle-commande") && data.contains("PAYEE"))) {
            label = I18n.t("dashboard.event.paid");
        } else if (event.contains("commande-creee") || event.contains("nouvelle-commande")) {
            label = I18n.t("dashboard.event.created");
        } else {
            label = event.isEmpty() ? I18n.t("dashboard.live") : event;
        }
        return data.isEmpty() ? label : label + " — " + data;
    }

    private void changerStatut(StatusOption statut) {
        int index = ordersList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= visibleOrders.size() || statut == null) return;
        if (isTerminal(visibleOrders.get(index))) {
            String locked = Ui.safe(I18n.t("dashboard.statusLocked"));
            feed.getItems().add(0, locked);
            Ui.toastError(this, locked);
            return;
        }
        String id = visibleOrders.get(index).id();
        var t = new Task<Order>() {
            @Override protected Order call() throws Exception {
                return ApiClient.get().patch("/orders/" + id + "/statut", java.util.Map.of("statut", statut.api()), Order.class);
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    Ui.toastSuccess(AdminDashboardView.this, I18n.t("dashboard.statusUpdated"));
                    load();
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    feed.getItems().add(0, err);
                    Ui.toastError(AdminDashboardView.this, err);
                });
            }
        };
        var thread = new Thread(t, "order-status");
        thread.setDaemon(true);
        thread.start();
    }

    private void annuler() {
        int index = ordersList.getSelectionModel().getSelectedIndex();
        if (index < 0 || index >= visibleOrders.size()) return;
        if (isTerminal(visibleOrders.get(index))) {
            String locked = Ui.safe(I18n.t("dashboard.statusLocked"));
            feed.getItems().add(0, locked);
            Ui.toastError(this, locked);
            return;
        }
        String id = visibleOrders.get(index).id();
        var t = new Task<Order>() {
            @Override protected Order call() throws Exception {
                return ApiClient.get().post("/orders/" + id + "/annuler", null, Order.class);
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> {
                    Ui.toastSuccess(AdminDashboardView.this, I18n.t("dashboard.cancelled"));
                    load();
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    feed.getItems().add(0, err);
                    Ui.toastError(AdminDashboardView.this, err);
                });
            }
        };
        var thread = new Thread(t, "order-cancel");
        thread.setDaemon(true);
        thread.start();
    }

    @Override public void dispose() { sse.stop(); }

    private record StatusOption(String api, String label) {
        @Override public String toString() { return label; }
    }
}
