package tn.cafe.pos.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import tn.cafe.pos.desktop.core.router.Router;
import tn.cafe.pos.desktop.core.theme.ThemeManager;

/** Entry point: ./mvnw javafx:run  •  1280x800 tactile, dark/light, MVVM + REST/SSE. */
public class DesktopPosApp extends Application {
    @Override
    public void start(Stage stage) {
        var router = new Router();
        router.go(Router.Route.MODE);
        var scene = new Scene(router.root(), 1280, 800);
        ThemeManager.apply(scene);
        stage.setTitle("POS Tunisie — Café & Restaurant (Client + Gérant)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        // Arabic shaping: T2K prism text keeps Arabic letters joined/ligated instead of
        // rendered detached with gaps; LCD text off avoids fringe on shaped glyphs.
        // Must be set before the JavaFX toolkit starts.
        System.setProperty("prism.text", System.getProperty("prism.text", "t2k"));
        System.setProperty("prism.lcdtext", System.getProperty("prism.lcdtext", "false"));
        launch(args);
    }
}
