package tn.cafe.pos.desktop.core.router;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import javafx.geometry.NodeOrientation;
import javafx.scene.layout.StackPane;
import tn.cafe.pos.desktop.core.i18n.I18n;
import tn.cafe.pos.desktop.view.*;

/** Tiny router: single StackPane root, navigate() swaps full-screen views. */
public class Router {
    public enum Route {
        MODE, CATALOG, CART, PAYMENT, PAYMENT_CONFIRM, QR_PAY, NFC_PAY, TICKET,
        ADMIN_LOGIN, ADMIN_DASH, CATALOG_ADMIN, TICKETS, SETTINGS
    }

    private final StackPane root = new StackPane();
    private final Deque<Route> history = new ArrayDeque<>();
    private Object param;
    private Route currentRoute;

    public StackPane root() { return root; }
    public Object param() { return param; }

    public void go(Route r) { go(r, null, true); }
    public void go(Route r, Object param) { go(r, param, true); }

    private void go(Route r, Object p, boolean push) {
        this.param = p;
        if (push) history.push(r);
        currentRoute = r;
        render();
    }

    /** Rebuilds the active screen after a language change while keeping its route data. */
    public void refresh() { render(); }

    private void render() {
        root.setNodeOrientation(I18n.isRtl() ? NodeOrientation.RIGHT_TO_LEFT : NodeOrientation.LEFT_TO_RIGHT);
        root.getChildren().stream()
                .filter(tn.cafe.pos.desktop.view.ViewLifecycle.class::isInstance)
                .map(tn.cafe.pos.desktop.view.ViewLifecycle.class::cast)
                .forEach(tn.cafe.pos.desktop.view.ViewLifecycle::dispose);
        root.getChildren().setAll(viewFor(currentRoute == null ? Route.MODE : currentRoute));
    }

    public void back() {
        if (history.size() > 1) { history.pop(); go(history.peek(), null, false); }
        else go(Route.MODE, null, false);
    }

    private javafx.scene.Node viewFor(Route r) {
        Supplier<javafx.scene.Node> s = switch (r) {
            case MODE -> () -> new ModeSelectView(this);
            case CATALOG -> () -> new CustomerCatalogView(this);
            case CART -> () -> new CartView(this);
            case PAYMENT -> () -> new PaymentView(this);
            case PAYMENT_CONFIRM -> () -> new PaymentConfirmationView(this);
            case QR_PAY -> () -> new QrPayView(this);
            case NFC_PAY -> () -> new NfcPayView(this);
            case TICKET -> () -> new TicketPreviewView(this);
            case ADMIN_LOGIN -> () -> new AdminLoginView(this);
            case ADMIN_DASH -> () -> new AdminDashboardView(this);
            case CATALOG_ADMIN -> () -> new CatalogAdminView(this);
            case TICKETS -> () -> new TicketsView(this);
            case SETTINGS -> () -> new SettingsView(this);
        };
        return s.get();
    }
}
