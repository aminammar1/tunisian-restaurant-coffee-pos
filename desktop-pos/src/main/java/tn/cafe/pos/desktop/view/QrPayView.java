package tn.cafe.pos.desktop.view;

import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Order;

/** S4a — QR payment simulation. The code is entered manually; no camera is required. */
public class QrPayView extends BorderPane {
    private final Label status = new Label(I18n.t("common.loading"));
    private java.util.function.Consumer<String> proximityAction = ignored -> {};

    public QrPayView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("qr.title"), true));
        Object p = router.param();
        Order order = p instanceof Order o ? o : (p instanceof PaymentView.PayCtx c ? c.order() : null);

        var codeField = new TextField();
        codeField.setPromptText(Ui.safe(I18n.t("qr.manual")));
        codeField.getStyleClass().add("search");
        codeField.setMaxWidth(480);

        var confirmBtn = Ui.big(I18n.t("qr.confirm"), "primary");
        HBox.setHgrow(confirmBtn, javafx.scene.layout.Priority.ALWAYS);
        Runnable submitPay = () -> confirmer(router, order, codeField.getText());
        confirmBtn.setOnAction(e -> submitPay.run());
        var row = new HBox(12, confirmBtn);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(480);

        status.setWrapText(false);
        status.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        status.setMaxWidth(480);
        status.getStyleClass().add("status");
        var center = new VBox(12, Ui.subtitle(order == null ? I18n.t("payment.demoNoOrder") : I18n.t("payment.order", order.numero(), order.total())),
            codeField, row, status);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(20));
        setCenter(Ui.vscroll(center));
    }

    private void confirmer(Router router, Order order, String code) {
        if (order == null) {
            status.setText(I18n.t("qr.noOrder"));
            Ui.toastError(this, I18n.t("qr.noOrder"));
            return;
        }
        status.setText(I18n.t("qr.confirming", code.isBlank() ? "none" : code));
        var t = new Task<Map<String, Object>>() {
            @Override protected Map<String, Object> call() throws Exception {
                return ApiClient.get().postMap("/payments/confirmer", Map.of("commandeId", order.id(), "type", "QR"));
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> PaymentView.showConfirmation(router, "QR", getValue()));
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    status.setText(err);
                    Ui.toastError(QrPayView.this, err);
                });
            }
        };
        var thread = new Thread(t, "pay-qr");
        thread.setDaemon(true);
        thread.start();
    }

}
