package tn.cafe.pos.desktop.core.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.io.IOException;
import tn.cafe.pos.desktop.config.AppConfig;

/**
 * Minimal REST client over java.net.http + Jackson.
 * Mirrors backend routes: /api/v1/{auth,categories,products,orders,payments,tickets}.
 */
public class ApiClient {
    private static final ApiClient INSTANCE = new ApiClient();
    public static ApiClient get() { return INSTANCE; }

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ObjectMapper mapper() { return mapper; }

    private HttpRequest.Builder base(String path) {
        var b = HttpRequest.newBuilder(URI.create(AppConfig.apiBase() + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        String token = AuthSession.get().token();
        if (token != null && !token.isBlank()) b.header("Authorization", "Bearer " + token);
        return b;
    }

    public <T> T get(String path, Class<T> type) throws IOException, InterruptedException, ApiException {
        var res = http.send(base(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        check(res);
        return mapper.readValue(res.body(), type);
    }

    public <T> T get(String path, TypeReference<T> type) throws IOException, InterruptedException, ApiException {
        var res = http.send(base(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        check(res);
        return mapper.readValue(res.body(), type);
    }

    public <T> java.util.List<T> getList(String path, Class<T> elementType) throws IOException, InterruptedException, ApiException {
        var res = http.send(base(path).GET().build(), HttpResponse.BodyHandlers.ofString());
        check(res);
        var listType = mapper.getTypeFactory().constructCollectionType(java.util.List.class, elementType);
        return mapper.readValue(res.body(), listType);
    }

    public <T> T post(String path, Object body, Class<T> type) throws IOException, InterruptedException, ApiException {
        String json = body == null ? "{}" : mapper.writeValueAsString(body);
        var res = http.send(base(path).POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
        check(res);
        if (type == Void.class || res.body() == null || res.body().isBlank()) return null;
        return mapper.readValue(res.body(), type);
    }

    /** JSON-object variant that preserves key/value types and avoids raw Map callers. */
    public java.util.Map<String, Object> postMap(String path, Object body) throws IOException, InterruptedException, ApiException {
        String json = body == null ? "{}" : mapper.writeValueAsString(body);
        var res = http.send(base(path).POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
        check(res);
        return mapper.readValue(res.body(), new TypeReference<>() { });
    }

    public <T> T patch(String path, Object body, Class<T> type) throws IOException, InterruptedException, ApiException {
        String json = body == null ? "{}" : mapper.writeValueAsString(body);
        var res = http.send(base(path).method("PATCH", HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
        check(res);
        return mapper.readValue(res.body(), type);
    }

    public <T> T put(String path, Object body, Class<T> type) throws IOException, InterruptedException, ApiException {
        String json = body == null ? "{}" : mapper.writeValueAsString(body);
        var res = http.send(base(path).PUT(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
        check(res);
        return mapper.readValue(res.body(), type);
    }

    public void delete(String path) throws IOException, InterruptedException, ApiException {
        var res = http.send(base(path).DELETE().build(), HttpResponse.BodyHandlers.ofString());
        check(res);
    }

    /** Raw GET for SSE / plain text. */
    public HttpClient http() { return http; }

    private static void check(HttpResponse<String> res) throws ApiException {
        if (res.statusCode() >= 200 && res.statusCode() < 300) return;
        throw new ApiException(res.statusCode(), res.body());
    }

    public static class ApiException extends Exception {
        public final int status;
        public final String body;
        public ApiException(int status, String body) {
            super(message(status, body));
            this.status = status; this.body = body;
        }

        private static String message(int status, String body) {
            if (body == null) return "HTTP " + status;
            String detail = body.length() > 300 ? body.substring(0, 300) : body;
            return "HTTP " + status + " " + detail;
        }
    }
}
