package tn.cafe.pos.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Emits the two startup facts an operator needs, without exposing connection secrets. */
@Component
public class StartupLog {
    private static final Logger log = LoggerFactory.getLogger(StartupLog.class);

    @Value("${server.port}")
    private int port;

    @Value("${spring.mongodb.database}")
    private String database;

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        // ApplicationReady happens after AdminSeeder has made a MongoDB request successfully.
        log.info("[mongo] connected database={}", database);
        log.info("[pos] API ready http://localhost:{}/api/v1  |  docs /swagger-ui.html", port);
    }
}
