package tn.cafe.pos.desktop.view;

import java.util.Map;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.model.Order;

/**
 * S4b — Détection NFC / infrarouge simulée: radar animé + vague.
 * Bouton "Simuler détection" -> POST /payments/proximite {commandeId, signalDetecte:true}.
 */
public class NfcPayView extends BorderPane {
    private final Label status = new Label(I18n.t("nfc.start"));

    public NfcPayView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("nfc.title"), true));
        Object p = router.param();
        Order order = p instanceof Order o ? o : (p instanceof PaymentView.PayCtx c ? c.order() : null);

        var radar = new StackPane();
        radar.setMinSize(300, 300); radar.setMaxSize(300, 300);
        for (int i = 3; i >= 1; i--) {
            var c = new Circle(130 - i * 30);
            c.getStyleClass().add("radar-" + i);
            radar.getChildren().add(c);
            pulse(c, i * 500);
        }
        var signal = new Label("NFC");
        signal.getStyleClass().add("signal-label");
        radar.getChildren().add(signal);

        var detect = Ui.big(I18n.t("nfc.detect"), "primary");
        detect.setMaxWidth(360);
        detect.setOnAction(e -> {
            status.setText(Ui.safe(I18n.t("nfc.detected")));
            flash(radar);
            confirmer(router, order);
        });

        var manual = new Button(Ui.safe(I18n.t("nfc.manual")));
        manual.getStyleClass().add("link");
        manual.setWrapText(false);
        manual.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        manual.setOnAction(e -> confirmer(router, order));

        status.setWrapText(false);
        status.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        status.setMaxWidth(480);
        status.getStyleClass().add("status");
        var center = new VBox(16,
                Ui.subtitle(order == null ? I18n.t("payment.demoNoOrder") : I18n.t("payment.order", order.numero(), order.total())),
                radar, status, detect, manual);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(24));
        setCenter(Ui.vscroll(center));
    }

    private static void pulse(Circle c, int delay) {
        var st = new ScaleTransition(Duration.millis(1400), c);
        st.setFromX(0.85); st.setToX(1.12); st.setFromY(0.85); st.setToY(1.12);
        st.setAutoReverse(true); st.setCycleCount(ScaleTransition.INDEFINITE);
        st.setDelay(Duration.millis(delay));
        st.play();
    }

    private void flash(StackPane radar) {
        radar.getStyleClass().add("radar-hit");
        var pause = new javafx.animation.PauseTransition(Duration.millis(900));
        pause.setOnFinished(e -> radar.getStyleClass().remove("radar-hit"));
        pause.play();
    }

    private void confirmer(Router router, Order order) {
        if (order == null) {
            status.setText(I18n.t("nfc.noOrder"));
            Ui.toastError(this, I18n.t("nfc.noOrder"));
            return;
        }
        var t = new Task<Map<String, Object>>() {
            @Override protected Map<String, Object> call() throws Exception {
                return ApiClient.get().postMap("/payments/proximite", Map.of("commandeId", order.id(), "signalDetecte", true));
            }
            @Override protected void succeeded() {
                Platform.runLater(() -> PaymentView.showConfirmation(router, "INFRARED", getValue()));
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    status.setText(err);
                    Ui.toastError(NfcPayView.this, err);
                });
            }
        };
        var thread = new Thread(t, "pay-nfc");
        thread.setDaemon(true);
        thread.start();
    }
}
