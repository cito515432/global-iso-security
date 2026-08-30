package com.globalisosecurity.backend.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalisosecurity.backend.dto.RpmMlFeatureRequest;
import com.globalisosecurity.backend.dto.RpmMlPredictionResponse;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RpmMlClientService {
    private static final Logger log = LoggerFactory.getLogger(RpmMlClientService.class);

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final boolean enabled;
    private final String baseUrl;
    private final String apiKey;
    private final int timeoutSeconds;

    public RpmMlClientService(
            ObjectMapper objectMapper,
            @Value("${app.rpm.ml.enabled:false}") boolean enabled,
            @Value("${app.rpm.ml.url:}") String baseUrl,
            @Value("${app.rpm.ml.api-key:}") String apiKey,
            @Value("${app.rpm.ml.timeout-seconds:120}") int timeoutSeconds) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.baseUrl = baseUrl == null ? "" : baseUrl.replaceAll("/+$", "");
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.timeoutSeconds = Math.max(10, timeoutSeconds);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(20, this.timeoutSeconds)))
                .build();
        if (enabled && !configurado()) {
            log.error("RPM ML fue habilitado pero RPM_ML_URL o RPM_ML_API_KEY no cumplen la configuración segura");
        }
    }

    public boolean configurado() {
        return enabled && !baseUrl.isBlank() && apiKey.length() >= 32 && urlPermitida(baseUrl);
    }

    public List<RpmMlPredictionResponse.Prediction> predecirLote(List<RpmMlFeatureRequest> items) {
        if (!configurado() || items == null || items.isEmpty()) return List.of();
        try {
            String body = objectMapper.writeValueAsString(Map.of("items", items));
            HttpRequest request = authenticatedRequest(baseUrl + "/predict/batch")
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("RPM ML respondió HTTP {}", response.statusCode());
                return List.of();
            }
            RpmMlPredictionResponse parsed = objectMapper.readValue(response.body(), RpmMlPredictionResponse.class);
            return parsed.predictions() == null ? List.of() : parsed.predictions();
        } catch (Exception ex) {
            log.warn("No fue posible consultar el servicio RPM ML. El motor determinista continúa disponible: {}", ex.getClass().getSimpleName());
            return List.of();
        }
    }

    public Map<String, Object> estadoRemoto() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", enabled);
        out.put("configured", configurado());
        if (!configurado()) {
            out.put("status", "DISABLED");
            return out;
        }
        try {
            HttpRequest request = authenticatedRequest(baseUrl + "/metadata")
                    .timeout(Duration.ofSeconds(Math.min(timeoutSeconds, 60)))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            out.put("httpStatus", response.statusCode());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode json = objectMapper.readTree(response.body());
                out.put("status", "AVAILABLE");
                out.put("metadata", objectMapper.convertValue(json, Map.class));
            } else {
                out.put("status", "UNAVAILABLE");
            }
        } catch (Exception ex) {
            out.put("status", "UNAVAILABLE");
        }
        return out;
    }

    private HttpRequest.Builder authenticatedRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("X-ML-API-Key", apiKey);
    }

    private boolean urlPermitida(String value) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
            if ("https".equals(scheme)) return true;
            return "http".equals(scheme)
                    && ("localhost".equals(host) || "127.0.0.1".equals(host) || "ml-service".equals(host));
        } catch (Exception ex) {
            return false;
        }
    }
}
