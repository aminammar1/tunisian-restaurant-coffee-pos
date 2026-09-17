package tn.cafe.pos.desktop.view;

import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.model.Order;
import tn.cafe.pos.desktop.viewmodel.CartStore;

/**
 * S5 — Aperçu tickets 58mm: SERVICE (cuisine) + CLIENT (reçu).
 * Reçoit Order, Map{paiement,commande,tickets} ou PayCtx (paiement direct simulé).
 */
public class TicketPreviewView extends BorderPane {
    public TicketPreviewView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("ticket.title"), true));
        Object p = router.param();

        String serviceTxt = "—";
        String clientTxt = "—";
        String header = I18n.t("payment.choose");
        boolean pendingPayment = false;

        if (p instanceof Map<?, ?> response && response.get("tickets") instanceof List<?> tickets) {
            for (Object value : tickets) {
                if (value instanceof Map<?, ?> ticket) {
                    Object typeValue = ticket.get("type");
                    Object contentValue = ticket.get("contenu");
                    String type = typeValue == null ? "" : typeValue.toString();
                    String contenu = contentValue == null ? "" : contentValue.toString();
                    if (type.contains("SERVICE")) serviceTxt = contenu;
                    else clientTxt = contenu;
                }
            }
            header = I18n.t("ticket.paid");
            CartStore.get().clear();
        } else if (p instanceof Order o) {
            header = I18n.t("ticket.pending", o.numero());
            clientTxt = I18n.t("ticket.pendingClient");
            serviceTxt = I18n.t("ticket.pendingService");
            pendingPayment = true;
        } else if (p instanceof PaymentView.PayCtx ctx) {
            header = I18n.t("ticket.demo", ctx.type());
            if (ctx.order() != null) { clientTxt = previewLocal(ctx.order()); serviceTxt = previewService(ctx.order()); }
            CartStore.get().clear();
        }
        final boolean paymentPending = pendingPayment;

        var center = new FlowPane();
        center.setHgap(20);
        center.setVgap(16);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(20));
        center.setPrefWrapLength(720);
        center.getChildren().addAll(ticketPane(I18n.t("ticket.service"), serviceTxt),
                ticketPane(I18n.t("ticket.client"), clientTxt));

        var top = new VBox(6, Ui.title(header), Ui.subtitle(I18n.t("ticket.printer", tn.cafe.pos.desktop.config.AppConfig.printerName())));
        top.setAlignment(Pos.CENTER);
        top.setPadding(new Insets(16, 16, 0, 16));

        var print = Ui.big(I18n.t("ticket.print"), "accent");
        HBox.setHgrow(print, Priority.ALWAYS);
        print.setDisable(paymentPending);
        print.setOnAction(e -> Ui.toast(this, I18n.t("ticket.printed", tn.cafe.pos.desktop.config.AppConfig.printerName())));
        var again = Ui.big(I18n.t("ticket.new"), "primary");
        HBox.setHgrow(again, Priority.ALWAYS);
        again.setOnAction(e -> router.go(Router.Route.CATALOG));
        var actions = new HBox(12);
        if (paymentPending && p instanceof Order order) {
            var payNow = Ui.big(I18n.t("ticket.payNow"), "primary");
            HBox.setHgrow(payNow, Priority.ALWAYS);
            payNow.setOnAction(e -> router.go(Router.Route.PAYMENT, order));
            actions.getChildren().add(payNow);
        }
        actions.getChildren().addAll(print, again);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(0, 16, 18, 16));
        actions.setMaxWidth(720);

        var actionsWrap = new HBox(actions);
        actionsWrap.setAlignment(Pos.CENTER);
        var wrap = new VBox(top, Ui.vscroll(center));
        VBox.setVgrow(wrap.getChildren().get(1), Priority.ALWAYS);
        setCenter(wrap);
        setBottom(actionsWrap);
    }

    private static VBox ticketPane(String title, String body) {
        var box = new VBox(8);
        box.getStyleClass().add("ticket");
        box.setPadding(new Insets(16));
        box.setMinWidth(300); box.setMaxWidth(380);
        box.setPrefWidth(340);
        var t = Ui.oneLine(title, "card-title");
        t.setMaxWidth(340);
        var area = new TextArea(Ui.safe(body));
        area.setEditable(false);
        area.setWrapText(false);
        area.getStyleClass().add("ticket-text");
        area.setPrefRowCount(20);
        area.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(t, area);
        return box;
    }

    private static String previewLocal(Order o) {
        var sb = new StringBuilder("*** CAFE TUNISIE ***\n").append(I18n.t("ticket.receipt")).append("\n");
        sb.append("N° ").append(o.numero()).append("\n");
        o.items().forEach(i -> sb.append(i.quantite()).append("x ").append(i.nomProduit())
                .append("  ").append(i.sousTotal()).append("\n"));
        sb.append("----------------\nTOTAL: ").append(o.total()).append(" TND\n").append(I18n.t("ticket.thanks")).append("\n");
        return sb.toString();
    }

    private static String previewService(Order o) {
        var sb = new StringBuilder("*** CUISINE ***\n");
        sb.append("N° ").append(o.numero()).append("  ").append(o.tableOuClient()).append("\n");
        o.items().forEach(i -> sb.append("[").append(i.quantite()).append("] ").append(i.nomProduit()).append("\n"));
        return sb.toString();
    }
}
