package com.globalisosecurity.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalisosecurity.backend.dto.RpmMlFeatureRequest;
import com.globalisosecurity.backend.dto.RpmMlPredictionResponse;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.RpmAnalisis;
import com.globalisosecurity.backend.repositories.RpmAnalisisRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RpmMlPredictionService {
    private final RpmAnalisisRepository analisisRepository;
    private final RpmMlFeatureService featureService;
    private final RpmMlClientService client;
    private final AccesoEmpresaService acceso;
    private final ObjectMapper objectMapper;
    private final LogAuditoriaService logs;

    public RpmMlPredictionService(
            RpmAnalisisRepository analisisRepository,
            RpmMlFeatureService featureService,
            RpmMlClientService client,
            AccesoEmpresaService acceso,
            ObjectMapper objectMapper,
            LogAuditoriaService logs) {
        this.analisisRepository = analisisRepository;
        this.featureService = featureService;
        this.client = client;
        this.acceso = acceso;
        this.objectMapper = objectMapper;
        this.logs = logs;
    }

    public Map<String, Object> predecirServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        List<RpmAnalisis> latest = ultimosPorControl(servicioId);
        int actualizados = predecirYGuardar(latest);
        logs.registrarLog("PREDECIR", "RPM_ML", "Predicción ML del servicio " + servicioId + ": " + actualizados + " análisis actualizados");
        return Map.of(
                "servicioId", servicioId,
                "analisis", latest.size(),
                "mlActualizados", actualizados,
                "mlConfigurado", client.configurado());
    }

    public Map<String, Object> predecirSoa(Long soaId) {
        RpmAnalisis a = analisisRepository.findFirstBySoaControlIdOrderByGeneradoEnDesc(soaId)
                .orElseThrow(() -> new ResourceNotFoundException("El control aún no tiene un análisis RPM"));
        acceso.servicioAutorizado(a.getServicio().getId());
        int actualizados = predecirYGuardar(List.of(a));
        return Map.of(
                "analisisId", a.getId(),
                "mlActualizados", actualizados,
                "mlConfigurado", client.configurado());
    }

    public Map<String, Object> estado() {
        return client.estadoRemoto();
    }

    private int predecirYGuardar(List<RpmAnalisis> analisis) {
        if (!client.configurado() || analisis.isEmpty()) return 0;
        List<RpmMlFeatureRequest> features = analisis.stream()
                .filter(a -> a.getSoaControl() != null)
                .map(featureService::construir)
                .toList();
        List<RpmMlPredictionResponse.Prediction> predictions = client.predecirLote(features);
        if (predictions.isEmpty()) return 0;

        Map<Long, RpmAnalisis> byId = analisis.stream()
                .collect(Collectors.toMap(RpmAnalisis::getId, x -> x, (a, b) -> a));
        int updated = 0;
        for (RpmMlPredictionResponse.Prediction p : predictions) {
            if (p.analysisId() == null) continue;
            RpmAnalisis a = byId.get(p.analysisId());
            if (a == null) continue;
            a.setPrioridadMl(p.priority());
            a.setConfianzaMl(p.confidence());
            a.setVersionModeloMl(p.modelVersion());
            a.setMlEstado("DISPONIBLE");
            a.setMlGeneradoEn(LocalDateTime.now());
            try {
                a.setProbabilidadesMl(objectMapper.writeValueAsString(p.probabilities() == null ? Map.of() : p.probabilities()));
            } catch (Exception ex) {
                a.setProbabilidadesMl("{}");
            }
            analisisRepository.save(a);
            updated++;
        }
        return updated;
    }

    private List<RpmAnalisis> ultimosPorControl(Long servicioId) {
        Map<Long, RpmAnalisis> latest = new LinkedHashMap<>();
        for (RpmAnalisis a : analisisRepository.findByServicioIdOrderByGeneradoEnDesc(servicioId)) {
            Long key = a.getSoaControl() != null ? a.getSoaControl().getId() : -a.getId();
            latest.putIfAbsent(key, a);
        }
        return new ArrayList<>(latest.values());
    }
}
