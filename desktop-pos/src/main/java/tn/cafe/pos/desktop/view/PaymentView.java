package tn.cafe.pos.desktop.view;

import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Order;

/**
 * S3 — Choix paiement simulé: QR / Apple Pay / Carte / Cash / Infrarouge-NFC.
 * Reçoit Order en param (peut être null en démo).
 */
public class PaymentView extends BorderPane {
    private final Label status = new Label();

    public PaymentView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("payment.title"), true));
        Object p = router.param();
        Order order = p instanceof Order o ? o : null;

        var center = new VBox(14);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(24));
        center.getChildren().add(Ui.title(order == null ? I18n.t("payment.choose") : I18n.t("payment.order", order.numero(), order.total())));
        center.getChildren().add(Ui.subtitle(I18n.t("payment.subtitle")));

        var grid = new FlowPane();
        grid.setHgap(16); grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);
        grid.setMaxWidth(720);
        grid.setPrefWrapLength(640);
        grid.getChildren().addAll(
            payCard(router, order, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.QRCODE, I18n.t("payment.qr"), I18n.t("payment.qr.desc"), "QR"),
            payCard(router, order, org.kordamp.ikonli.materialdesign2.MaterialDesignN.NFC, I18n.t("payment.nfc"), I18n.t("payment.nfc.desc"), "INFRARED"),
            payCard(router, order, org.kordamp.ikonli.fontawesome5.FontAwesomeBrands.APPLE_PAY, I18n.t("payment.apple"), I18n.t("payment.apple.desc"), "APPLE_PAY"),
            payCard(router, order, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.CREDIT_CARD, I18n.t("payment.card"), I18n.t("payment.card.desc"), "CARD"),
            payCard(router, order, org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.MONEY_BILL_WAVE, I18n.t("payment.cash"), I18n.t("payment.cash.desc"), "CASH"));
        status.setWrapText(false);
        status.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        status.setMaxWidth(Ui.MAX_TEXT_WIDTH);
        status.getStyleClass().add("status");
        center.getChildren().addAll(grid, status);
        setCenter(Ui.vscroll(center));
    }

    private VBox payCard(Router router, Order order, org.kordamp.ikonli.Ikon ikon, String title, String desc, String type) {
        var c = new VBox(8);
        c.getStyleClass().add("mode-card");
        c.setAlignment(Pos.CENTER);
        c.setPadding(new Insets(18));
        // Fixed size: every payment method renders an identical tile.
        c.setMinSize(280, 236);
        c.setPrefSize(280, 236);
        c.setMaxSize(280, 236);
        var glyph = Ui.icon(ikon, 44);
        var t = Ui.oneLine(title, "card-title");
        t.setMaxWidth(240);
        t.setAlignment(Pos.CENTER);
        var d = Ui.oneLine(desc, "subtitle");
        d.setMaxWidth(240);
        d.setAlignment(Pos.CENTER);
        var b = Ui.big(I18n.t("payment.pay", type), "primary");
        b.setOnAction(ev -> {
            if ("QR".equals(type)) router.go(Router.Route.QR_PAY, order == null ? type : new PayCtx(order, type));
            else if ("INFRARED".equals(type)) router.go(Router.Route.NFC_PAY, order == null ? type : new PayCtx(order, type));
            else confirmer(router, order, type);
        });
        c.getChildren().addAll(glyph, t, d, b);
        return c;
    }

    /** Every direct channel uses the same simulated backend contract as QR/NFC. */
    private void confirmer(Router router, Order order, String type) {
        if (order == null) {
            status.setText(I18n.t("payment.needOrder"));
            Ui.toastError(this, I18n.t("payment.needOrder"));
            return;
        }
        status.setText(I18n.t("payment.creating", type));
        var task = new Task<Map<String, Object>>() {
            @Override protected Map<String, Object> call() throws Exception {
                return ApiClient.get().postMap("/payments/confirmer",
                        Map.of("commandeId", order.id(), "type", type));
            }

            @Override protected void succeeded() {
                Platform.runLater(() -> router.go(Router.Route.TICKET, getValue()));
            }

            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    status.setText(err);
                    Ui.toastError(PaymentView.this, err);
                });
            }
        };
        Thread worker = new Thread(task, "pay-" + type.toLowerCase());
        worker.setDaemon(true);
        worker.start();
    }

    /** Contexte paiement transmis aux écrans suivants. */
    public record PayCtx(Order order, String type) {}
}
