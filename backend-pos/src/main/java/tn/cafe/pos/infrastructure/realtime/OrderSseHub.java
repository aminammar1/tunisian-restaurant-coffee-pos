package tn.cafe.pos.infrastructure.realtime;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.cafe.pos.domain.model.Order;

/**
 * Hub SSE : les gérants s'abonnent, chaque nouvelle commande est diffusée.
 * Thread-safe via CopyOnWriteArrayList.
 */
@Service
public class OrderSseHub {
    private final List<SseEmitter> clients = new CopyOnWriteArrayList<>();

    public SseEmitter souscrire() {
        SseEmitter e = new SseEmitter(0L);
        clients.add(e);
        e.onCompletion(() -> clients.remove(e));
        e.onTimeout(() -> clients.remove(e));
        try { e.send(SseEmitter.event().name("connecte").data("ok")); }
        catch (IOException ignored) {}
        return e;
    }

    public void diffuser(Order order) {
        String payload = "Nouvelle commande " + (order.getNumero() != null ? order.getNumero() : order.getId())
                + " - Total " + order.getTotal() + " - " + order.getStatut();
        for (SseEmitter e : clients) {
            try { e.send(SseEmitter.event().name("nouvelle-commande").data(payload)); }
            catch (Exception ex) { clients.remove(e); }
        }
    }
}
