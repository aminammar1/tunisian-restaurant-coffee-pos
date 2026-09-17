package tn.cafe.pos.desktop.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.core.router.Router;

/** S0 — mode choice: customer touch kiosk / manager admin. Scrollable, RTL-safe. */
public class ModeSelectView extends BorderPane {
    public ModeSelectView(Router router) {
        getStyleClass().add("root");
        setTop(new VBox(Ui.topBar(router, I18n.t("app.brand"), false), Ui.motifStrip()));

        var center = new VBox(18);
        center.setAlignment(Pos.TOP_CENTER);
        center.setPadding(new Insets(24, 24, 28, 24));

        var banner = Ui.tunisiaBanner(560, 190);

        var hero = Ui.oneLine(I18n.t("mode.welcome"), "hero");
        hero.setAlignment(Pos.CENTER);
        hero.setMaxWidth(Ui.MAX_TEXT_WIDTH);

        var sub = Ui.subtitle(I18n.t("mode.subtitle"));

        var cards = new FlowPane();
        cards.setHgap(20);
        cards.setVgap(16);
        cards.setAlignment(Pos.CENTER);
        cards.setMaxWidth(820);

        var clientCard = modeCard(I18n.t("mode.customer"),
                I18n.t("mode.customer.desc"), I18n.t("mode.customer.cta"), "mode-card",
                Ui.icon(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USERS, 52));
        clientCard.setOnMouseClicked(e -> router.go(Router.Route.CATALOG));

        var gerantCard = modeCard(I18n.t("mode.manager"),
                I18n.t("mode.manager.desc"), I18n.t("mode.manager.cta"), "red-card",
                Ui.iconOnRed(org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.USER_COG, 52));
        gerantCard.setOnMouseClicked(e -> router.go(Router.Route.ADMIN_LOGIN));

        cards.getChildren().addAll(clientCard, gerantCard);

        var hint = Ui.oneLine(I18n.t("mode.hint"), "hint");
        hint.setAlignment(Pos.CENTER);
        hint.setMaxWidth(Ui.MAX_TEXT_WIDTH);

        center.getChildren().addAll(banner, hero, sub, cards, hint);
        setCenter(Ui.vscroll(center));
    }

    private static VBox modeCard(String title, String desc, String cta, String cls, Node glyph) {
        var c = new VBox(10);
        c.getStyleClass().addAll("mode-card", cls);
        c.setAlignment(Pos.CENTER);
        c.setPadding(new Insets(24));
        c.setMinSize(300, 290);
        c.setPrefSize(340, 310);
        c.setMaxSize(380, 340);
        var t = Ui.oneLine(title, "card-title");
        t.setAlignment(Pos.CENTER);
        t.setMaxWidth(300);
        var d = Ui.paragraph(desc, "subtitle");
        d.setMaxWidth(300);
        var b = Ui.oneLine(cta, "cta");
        b.setAlignment(Pos.CENTER);
        b.setMaxWidth(300);
        c.getChildren().addAll(glyph, t, d, b);
        return c;
    }

    /** Router instantiates via Supplier<Node>; expose as Node. */
    public Node asNode() { return this; }
}
