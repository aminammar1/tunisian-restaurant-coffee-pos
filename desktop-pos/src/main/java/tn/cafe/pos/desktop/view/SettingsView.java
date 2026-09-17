package tn.cafe.pos.desktop.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.config.AppConfig;
import tn.cafe.pos.desktop.core.api.AuthSession;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;

/** S11 — Réglages: backend URL (info), imprimante, thème, déconnexion. */
public class SettingsView extends BorderPane {
    public SettingsView(Router router) {
        getStyleClass().add("root");
        setTop(Ui.topBar(router, I18n.t("settings.title"), true));
        var center = new VBox(12);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(24));

        var api = new TextField(AppConfig.apiBase());
        api.setPromptText("http://localhost:8080/api/v1");
        api.getStyleClass().add("search");
        api.setMaxWidth(420);
        var note = Ui.subtitle(I18n.t("settings.api.note", AppConfig.apiBase()));

        var printer = new TextField(AppConfig.printerName());
        printer.getStyleClass().add("search");
        printer.setMaxWidth(420);

        var theme = Ui.big(I18n.t("settings.theme"), "accent");
        theme.setMaxWidth(420);
        theme.setOnAction(e -> {
            var sc = getScene();
            if (sc != null) tn.cafe.pos.desktop.core.theme.ThemeManager.toggle(sc);
        });

        var language = new ComboBox<I18n.Language>();
        language.getItems().addAll(I18n.Language.values());
        language.setValue(I18n.current());
        language.setMaxWidth(420);
        language.setOnAction(e -> { I18n.set(language.getValue()); router.refresh(); });

        var logout = new Button(Ui.safe(I18n.t("settings.logout")));
        logout.getStyleClass().addAll("btn", "danger");
        logout.setMinHeight(52);
        logout.setMnemonicParsing(false);
        logout.setMaxWidth(420);
        logout.setOnAction(e -> { AuthSession.get().clear(); router.go(Router.Route.MODE); });

        var who = new Label(I18n.t("settings.session", AuthSession.get().isLoggedIn()
                ? AuthSession.get().usernameProperty().get() : I18n.t("settings.guest")));
        who.getStyleClass().add("subtitle");

        center.getChildren().addAll(Ui.title(I18n.t("settings.heading")), who,
                Ui.section(I18n.t("settings.api")), api, note,
                Ui.section(I18n.t("settings.printer")), printer,
                Ui.section(I18n.t("settings.language")), language, theme, logout);
        setCenter(Ui.vscroll(center));
    }
}
