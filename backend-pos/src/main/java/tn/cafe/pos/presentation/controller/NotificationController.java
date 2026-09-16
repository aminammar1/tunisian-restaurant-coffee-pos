package tn.cafe.pos.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tn.cafe.pos.infrastructure.realtime.OrderSseHub;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Temps réel", description = "SSE : le gérant reçoit les nouvelles commandes")
public class NotificationController {
    private final OrderSseHub hub;
    public NotificationController(OrderSseHub hub) { this.hub = hub; }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "S'abonner aux nouvelles commandes (SSE)")
    public SseEmitter stream() { return hub.souscrire(); }
}
