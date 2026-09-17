package tn.cafe.pos.desktop.view;

import java.util.Map;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Order;
import tn.cafe.pos.desktop.service.QrScannerService;
import com.github.sarxos.webcam.Webcam;

/** S4a — Scan QR paiement via webcam + fallback saisie manuelle. Puis POST /payments/confirmer {commandeId, type:QR}. */
public class QrPayView extends BorderPane implements ViewLifecycle {
    private final QrScannerService scanner = new QrScannerService();
    private Webcam cam;
    private final Label status = new Label(I18n.t("common.loading"));
    private java.util.function.Consumer<String> proximityAction = ignored -> {};

    public QrPayView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("qr.title"), true));
        Object p = router.param();
        Order order = p instanceof Order o ? o : (p instanceof PaymentView.PayCtx c ? c.order() : null);

        var preview = new ImageView();
        preview.setFitWidth(480); preview.setFitHeight(320);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        preview.getStyleClass().add("cam");

        var codeField = new TextField();
        codeField.setPromptText(Ui.safe(I18n.t("qr.manual")));
        codeField.getStyleClass().add("search");
        codeField.setMaxWidth(480);

        var scanBtn = Ui.big(I18n.t("login.startCamera"), "accent");
        HBox.setHgrow(scanBtn, javafx.scene.layout.Priority.ALWAYS);
        scanBtn.setOnAction(e -> startCam(preview, codeField));

        var confirmBtn = Ui.big(I18n.t("qr.confirm"), "primary");
        HBox.setHgrow(confirmBtn, javafx.scene.layout.Priority.ALWAYS);
        Runnable submitPay = () -> confirmer(router, order, codeField.getText());
        confirmBtn.setOnAction(e -> submitPay.run());
        // Object-close simulation, same as manager QR login: dark frames from a
        // hand/object in front of the lens auto-confirm when a code is present.
        proximityAction = ignored -> {
            if (codeField.getText().isBlank()) status.setText(Ui.safe(I18n.t("login.qr.detected")));
            else submitPay.run();
        };

        var simulateBtn = Ui.big(I18n.t("login.qr.simulate"), "accent");
        HBox.setHgrow(simulateBtn, javafx.scene.layout.Priority.ALWAYS);
        simulateBtn.setOnAction(e -> proximityAction.accept("close"));

        var row = new HBox(12, scanBtn, confirmBtn, simulateBtn);
        row.setAlignment(Pos.CENTER);
        row.setMaxWidth(480);

        status.setWrapText(false);
        status.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        status.setMaxWidth(480);
        status.getStyleClass().add("status");
        var center = new VBox(12, Ui.subtitle(order == null ? I18n.t("payment.demoNoOrder") : I18n.t("payment.order", order.numero(), order.total())),
                preview, codeField, row, status);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(20));
        setCenter(Ui.vscroll(center));
        startCam(preview, codeField);
    }

    private void startCam(ImageView preview, TextField codeField) {
        status.setText(I18n.t("common.loading"));
        var open = new Thread(() -> {
            Webcam found;
            try { found = scanner.openDefault(); }
            catch (Exception ex) { found = null; }
            final Webcam camFound = found;
            javafx.application.Platform.runLater(() -> {
                if (camFound == null) { status.setText(Ui.safe(I18n.t("qr.noCamera"))); return; }
                cam = camFound;
                scanner.preview(cam, preview, code -> codeField.setText(code), s -> status.setText(Ui.safe(s)), proximityAction);
            });
        }, "qr-cam-open");
        open.setDaemon(true);
        open.start();
    }

    private void confirmer(Router router, Order order, String code) {
        if (order == null) { status.setText(I18n.t("qr.noOrder")); return; }
        status.setText(I18n.t("qr.confirming", code.isBlank() ? "none" : code));
        var t = new Task<Map<String, Object>>() {
            @Override protected Map<String, Object> call() throws Exception {
                return ApiClient.get().postMap("/payments/confirmer", Map.of("commandeId", order.id(), "type", "QR"));
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> { scanner.stop(); router.go(Router.Route.TICKET, getValue()); });
            }
            @Override protected void failed() {
                Platform.runLater(() -> status.setText(I18n.t("qr.failed", getException().getMessage())));
            }
        };
        new Thread(t, "pay-qr").start();
    }

    /** Called when navigating away — best effort camera release. */
    @Override protected void layoutChildren() { super.layoutChildren(); }
    @Override public void dispose() { scanner.stop(); }
}
