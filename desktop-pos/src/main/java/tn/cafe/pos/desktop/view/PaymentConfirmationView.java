package tn.cafe.pos.desktop.view;

import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;

/** Bank-style confirmation moment shown after card or cash payment succeeds. */
public final class PaymentConfirmationView extends BorderPane {
    private final Router router;

    public PaymentConfirmationView(Router router) {
        this.router = router;
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("payment.confirmation.title"), false));
        var confirmation = new StackPane();
        confirmation.setMinSize(180, 180);
        confirmation.setPrefSize(180, 180);
        var circle = new Circle(78, Color.web("#1e8e4d"));
        var check = new Label("✓");
        check.getStyleClass().add("payment-check");
        confirmation.getChildren().addAll(circle, check);

        var title = Ui.title(I18n.t("payment.confirmation.title"));
        var detail = Ui.subtitle(I18n.t("payment.confirmation.detail", method()));
        var center = new VBox(18, confirmation, title, detail);
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(40));
        setCenter(center);

        var pop = new ScaleTransition(Duration.millis(480), confirmation);
        pop.setFromX(.55); pop.setFromY(.55); pop.setToX(1); pop.setToY(1);
        var fade = new FadeTransition(Duration.millis(480), confirmation);
        fade.setFromValue(.2); fade.setToValue(1);
        var sequence = new SequentialTransition(new javafx.animation.ParallelTransition(pop, fade));
        sequence.setOnFinished(e -> {
            var parameter = router.param();
            if (parameter instanceof Confirmation confirmationData) {
                Platform.runLater(() -> router.go(Router.Route.TICKET, confirmationData.response()));
            }
        });
        sequence.play();
    }

    private String method() {
        Object value = routerParamType();
        return value == null ? "" : value.toString();
    }

    private Object routerParamType() {
        Object parameter = router.param();
        return parameter instanceof Confirmation confirmation ? confirmation.type() : null;
    }

    public record Confirmation(String type, Map<String, Object> response) {}
}
