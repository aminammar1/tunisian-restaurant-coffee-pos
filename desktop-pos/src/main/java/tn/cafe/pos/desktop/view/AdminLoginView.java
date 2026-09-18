package tn.cafe.pos.desktop.view;

import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.api.ApiClient;
import tn.cafe.pos.desktop.core.api.AuthSession;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

/** S6 — Connexion Gérant (FR): Mot de passe / PIN / badge QR. */
public class AdminLoginView extends BorderPane {
    private final Label status = new Label();

    public AdminLoginView(Router router) {
        getStyleClass().add("root");
        setTop(new VBox(Ui.topBar(router, I18n.t("login.title"), true), Ui.motifStrip()));
        var tabs = new TabPane();
        tabs.getStyleClass().add("tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Mot de passe
        var user = new TextField("admin"); user.setPromptText(I18n.t("login.username"));
        var pass = new PasswordField(); pass.setPromptText(I18n.t("login.password.placeholder"));
        styleAll(user, pass);
        var b1 = Ui.big(I18n.t("login.connect"), "primary");
        b1.setOnAction(e -> login(router, "/auth/login", Map.of("username", user.getText(), "password", pass.getText())));
        tabs.getTabs().add(tab(I18n.t("login.password"), new VBox(10, user, pass, b1, status)));

        // PIN
        var pinUser = new TextField("admin"); pinUser.setPromptText(I18n.t("login.username"));
        var pin = new PasswordField(); pin.setPromptText(I18n.t("login.pin.placeholder")); pin.setEditable(false);
        styleAll(pinUser, pin);
        var keypad = new GridPane(); keypad.setHgap(8); keypad.setVgap(8); keypad.setAlignment(Pos.CENTER);
        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "C"};
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            var button = new Label(key);
            button.getStyleClass().add("pin-key");
            button.setMinSize(72, 54); button.setAlignment(Pos.CENTER);
            button.setOnMouseClicked(e -> {
                if ("C".equals(key)) pin.clear();
                else if ("⌫".equals(key) && !pin.getText().isEmpty()) pin.deleteText(pin.getText().length() - 1, pin.getText().length());
                else if (pin.getText().length() < 6) pin.appendText(key);
            });
            keypad.add(button, i % 3, i / 3);
        }
        var b2 = Ui.big(I18n.t("login.validatePin"), "primary");
        var pinStatus = new Label();
        var pinBox = new VBox(10, pinUser, pin, keypad, b2, pinStatus);
        // PinRequest deliberately requires a username; include it so PIN login is a real API call.
        b2.setOnAction(e -> loginTo(router, "/auth/pin", Map.of("username", pinUser.getText(), "pin", pin.getText()), pinStatus));
        tabs.getTabs().add(tab(I18n.t("login.pin"), pinBox));

        // QR
        var qrField = new TextField(); qrField.setPromptText(I18n.t("login.qr.placeholder"));
        styleAll(qrField);
        var qrStatus = new Label(I18n.t("login.qr.note"));
        var preview = new ImageView();
        preview.setFitWidth(220); preview.setFitHeight(220); preview.setPreserveRatio(false);
        preview.setSmooth(false);
        preview.getStyleClass().add("qr-image");
        var showQr = Ui.big(I18n.t("login.showQr"), "accent");
        var b3 = Ui.big(I18n.t("login.qrConnect"), "primary");
        Runnable submitQr = () -> loginTo(router, "/auth/qr", Map.of("qrKey", qrField.getText()), qrStatus);
        b3.setOnAction(e -> submitQr.run());
        showQr.setOnAction(e -> loadBackendQr(user.getText(), qrField, preview, qrStatus, showQr));
        var qrPreview = new VBox(8, Ui.title("QR"), preview);
        qrPreview.getStyleClass().add("qr-preview");
        qrPreview.setMaxWidth(Double.MAX_VALUE);
        preview.setFitWidth(380); preview.setFitHeight(200); preview.setPreserveRatio(true);
        preview.setPreserveRatio(true);
        var qrActions = new HBox(10, showQr, b3);
        qrActions.setAlignment(Pos.CENTER);
        HBox.setHgrow(showQr, Priority.ALWAYS); HBox.setHgrow(b3, Priority.ALWAYS);
        showQr.setMaxWidth(Double.MAX_VALUE); b3.setMaxWidth(Double.MAX_VALUE);
        qrStatus.setWrapText(false);
        qrStatus.setTextOverrun(javafx.scene.control.OverrunStyle.ELLIPSIS);
        qrStatus.setMaxWidth(420);
        var qrBox = new VBox(12, qrPreview, qrField, qrActions, qrStatus);
        qrBox.setMaxWidth(Double.MAX_VALUE);
        tabs.getTabs().add(tab(I18n.t("login.qr"), qrBox));

        var brandTitle = Ui.oneLine(I18n.t("app.short"), "title");
        brandTitle.setAlignment(Pos.CENTER);
        brandTitle.setMaxWidth(224);
        var brandTag = Ui.oneLine(I18n.t("app.tag"), "subtitle");
        brandTag.setAlignment(Pos.CENTER);
        brandTag.setMaxWidth(224);
        var brand = new VBox(14, Ui.sidebarBanner(224, 120), brandTitle, brandTag);
        brand.getStyleClass().add("login-brand");
        brand.setAlignment(Pos.CENTER);
        brand.setPrefWidth(280);
        brand.setMaxWidth(280);
        brand.setMinWidth(240);

        var access = new VBox(12, Ui.title(I18n.t("login.welcome")), Ui.subtitle(I18n.t("login.subtitle")), tabs);
        access.getStyleClass().add("login-panel");
        access.setMaxWidth(680);
        access.setMinWidth(0);
        HBox.setHgrow(access, Priority.ALWAYS);

        var center = new HBox(24, brand, access);
        center.getStyleClass().add("login-shell");
        center.setAlignment(Pos.CENTER);
        center.setPadding(new Insets(28, 32, 32, 32));
        var scroll = Ui.vscroll(center);
        scroll.setFitToHeight(true);
        setCenter(scroll);
    }

    private static Tab tab(String t, VBox content) {
        content.setPadding(new Insets(18)); content.setAlignment(Pos.CENTER);
        content.setFillWidth(true);
        var tab = new Tab(t, content);
        return tab;
    }
    private static void styleAll(javafx.scene.control.TextInputControl... fs) {
        for (var f : fs) { f.getStyleClass().add("search"); f.setMaxWidth(380); }
    }

    private void login(Router router, String path, Map<String, String> body) { loginTo(router, path, body, status); }

    private void loginTo(Router router, String path, Map<String, String> body, Label target) {
        target.setText(I18n.t("login.signing"));
        var t = new Task<Map<String, Object>>() {
            @Override protected Map<String, Object> call() throws Exception { return ApiClient.get().postMap(path, body); }
            @Override protected void succeeded() {
                Map<String, Object> r = getValue();
                String token = String.valueOf(r.getOrDefault("token", ""));
                String username = String.valueOf(r.getOrDefault("username", "gerant"));
                AuthSession.get().set(token, username, String.valueOf(r.getOrDefault("role", "GERANT")));
                Platform.runLater(() -> {
                    Ui.toastSuccess(AdminLoginView.this, I18n.t("dashboard.connected", username));
                    router.go(Router.Route.ADMIN_DASH);
                });
            }
            @Override protected void failed() {
                Platform.runLater(() -> {
                    String err = Ui.essentialError(getException());
                    target.setText(err);
                    Ui.toastError(AdminLoginView.this, err);
                });
            }
        };
        var thread = new Thread(t, "admin-login");
        thread.setDaemon(true);
        thread.start();
    }

    private static void renderQr(ImageView preview, String raw, Label status) {
        String value = raw == null ? "" : raw.strip();
        if (value.isBlank()) {
            status.setText(Ui.safe(I18n.t("login.qr.required")));
            return;
        }
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(value, BarcodeFormat.QR_CODE, 260, 260);
            var image = new javafx.scene.image.WritableImage(matrix.getWidth(), matrix.getHeight());
            for (int x = 0; x < matrix.getWidth(); x++) {
                for (int y = 0; y < matrix.getHeight(); y++) {
                    image.getPixelWriter().setColor(x, y, matrix.get(x, y)
                            ? javafx.scene.paint.Color.BLACK : javafx.scene.paint.Color.WHITE);
                }
            }
            preview.setImage(image);
            status.setText(Ui.safe(I18n.t("login.qr.ready")));
        } catch (Exception ex) {
            status.setText(Ui.safe(I18n.t("login.qr.invalid")));
        }
    }

    private static void loadBackendQr(String username, TextField qrField, ImageView preview,
            Label status, javafx.scene.control.Button trigger) {
        String account = username == null || username.isBlank() ? "admin" : username.strip();
        trigger.setDisable(true);
        status.setText(Ui.safe(I18n.t("login.qr.loading")));
        var task = new Task<Map<String, Object>>() {
            @Override protected Map<String, Object> call() throws Exception {
                String encoded = URLEncoder.encode(account, StandardCharsets.UTF_8);
                return ApiClient.get().get("/auth/qr-key?username=" + encoded,
                        new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
            }
            @Override protected void succeeded() {
                String key = String.valueOf(getValue().getOrDefault("qrKey", ""));
                qrField.setText(key);
                renderQr(preview, key, status);
                trigger.setDisable(false);
            }
            @Override protected void failed() {
                status.setText(Ui.essentialError(getException()));
                trigger.setDisable(false);
            }
        };
        var thread = new Thread(task, "manager-qr-create");
        thread.setDaemon(true);
        thread.start();
    }
}
