package tn.cafe.pos.desktop.view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Ticket;

/** S10 — Tickets (FR): liste + aperçu SERVICE / CLIENT. GET /tickets(?commandeId=). */
public class TicketsView extends BorderPane {
    private final ListView<String> list = new ListView<>();
    private final TextArea preview = new TextArea();
    private Ticket[] cache = new Ticket[0];

    public TicketsView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("tickets.title"), true));
        var filter = new TextField();
        filter.setPromptText(Ui.safe(I18n.t("tickets.filter")));
        filter.getStyleClass().add("search");
        filter.setMinWidth(0);
        var go = Ui.big(I18n.t("tickets.load"), "primary");
        HBox.setHgrow(go, Priority.NEVER);
        go.setMaxWidth(200);
        go.setOnAction(e -> load(filter.getText().isBlank() ? null : filter.getText().trim()));
        preview.setEditable(false);
        preview.setWrapText(false);
        preview.getStyleClass().add("ticket-text");
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> showSel());

        var left = new VBox(8, new HBox(10, filter, go), list);
        left.setPadding(new Insets(12, 0, 0, 16));
        left.setMinWidth(0);
        HBox.setHgrow(filter, Priority.ALWAYS);
        VBox.setVgrow(list, Priority.ALWAYS);
        var right = new VBox(8, Ui.section(I18n.t("tickets.preview")), preview);
        right.setPadding(new Insets(12, 16, 16, 0));
        right.setMinWidth(0);
        VBox.setVgrow(preview, Priority.ALWAYS);
        var split = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS); HBox.setHgrow(right, Priority.ALWAYS);
        split.setPadding(new Insets(0, 0, 16, 0));
        setCenter(split);
        load(null);
    }

    private void load(String commandeId) {
        var t = new Task<java.util.List<Ticket>>() {
            @Override protected java.util.List<Ticket> call() throws Exception {
                String path = commandeId == null ? "/tickets" : "/tickets?commandeId=" + commandeId;
                return ApiClient.get().getList(path, Ticket.class);
            }
            @Override protected void succeeded() {
                cache = getValue().toArray(new Ticket[0]);
                Platform.runLater(() -> {
                    list.getItems().clear();
                    for (Ticket x : cache) list.getItems().add(Ui.safe(x.type() + " | N°" + x.numeroCommande() + " | id=" + x.id()));
                    if (cache.length > 0) { list.getSelectionModel().select(0); showSel(); }
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> preview.setText(I18n.t("tickets.failed", Ui.friendlyError(getException()))));
            }
        };
        new Thread(t, "tickets-load").start();
    }

    private void showSel() {
        int i = list.getSelectionModel().getSelectedIndex();
        if (i >= 0 && i < cache.length) preview.setText(cache[i].contenu());
    }
}
