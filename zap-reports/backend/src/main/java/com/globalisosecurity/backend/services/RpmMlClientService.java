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
import java.util.*;
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
    }

    public boolean configurado() {
        return enabled && !baseUrl.isBlank();
    }

    public List<RpmMlPredictionResponse.Prediction> predecirLote(List<RpmMlFeatureRequest> items) {
        if (!configurado() || items == null || items.isEmpty()) return List.of();
        try {
            String body = objectMapper.writeValueAsString(Map.of("items", items));
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/predict/batch"))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (!apiKey.isBlank()) builder.header("X-ML-API-Key", apiKey);
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.warn("RPM ML respondió HTTP {}: {}", response.statusCode(), limitar(response.body(), 400));
                return List.of();
            }
            RpmMlPredictionResponse parsed = objectMapper.readValue(response.body(), RpmMlPredictionResponse.class);
            return parsed.predictions() == null ? List.of() : parsed.predictions();
        } catch (Exception ex) {
            log.warn("No fue posible consultar el servicio RPM ML. El motor determinista continúa disponible: {}", ex.getMessage());
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
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/metadata"))
                    .timeout(Duration.ofSeconds(Math.min(timeoutSeconds, 60)))
                    .GET().build();
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
            out.put("message", ex.getMessage());
        }
        return out;
    }

    private String limitar(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }
}
