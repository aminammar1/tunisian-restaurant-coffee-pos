package tn.cafe.pos.desktop.core.api;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** Holds JWT after gérant login (password / PIN / QR). Customer mode needs no token. */
public class AuthSession {
    private static final AuthSession INSTANCE = new AuthSession();
    public static AuthSession get() { return INSTANCE; }

    private final StringProperty token = new SimpleStringProperty();
    private final StringProperty username = new SimpleStringProperty();
    private final StringProperty role = new SimpleStringProperty();

    public String token() { return token.get(); }
    public StringProperty tokenProperty() { return token; }
    public StringProperty usernameProperty() { return username; }
    public StringProperty roleProperty() { return role; }
    public boolean isLoggedIn() { return token.get() != null && !token.get().isBlank(); }

    public void set(String token, String username, String role) {
        this.token.set(token); this.username.set(username); this.role.set(role);
    }
    public void clear() { set(null, null, null); }
}
