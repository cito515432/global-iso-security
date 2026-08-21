package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.RpmMlFeatureRequest;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class RpmMlFeatureService {
    private final PerfilOrganizacionalRepository perfilRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final HallazgoAuditoriaRepository hallazgoRepository;

    public RpmMlFeatureService(
            PerfilOrganizacionalRepository perfilRepository,
            EvidenciaRepository evidenciaRepository,
            HallazgoAuditoriaRepository hallazgoRepository) {
        this.perfilRepository = perfilRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.hallazgoRepository = hallazgoRepository;
    }

    public RpmMlFeatureRequest construir(RpmAnalisis analisis) {
        if (analisis == null || analisis.getSoaControl() == null) {
            throw new IllegalArgumentException("El análisis RPM no tiene un control SoA asociado");
        }
        SoaControl soa = analisis.getSoaControl();
        Servicio servicio = analisis.getServicio();
        Riesgo riesgo = analisis.getRiesgo();

        List<Evidencia> evidencias = evidenciaRepository.findBySoaControlIdOrderByFechaCargaDesc(soa.getId());
        List<HallazgoAuditoria> hallazgos = hallazgoRepository.findBySoaControlIdAndEstadoNot(soa.getId(), "CERRADO");
        PerfilOrganizacional perfil = perfilRepository.findByEmpresaId(servicio.getEmpresa().getId()).orElse(null);

        String sector = null;
        if (perfil != null && perfil.getSector() != null) sector = perfil.getSector().getNombre();
        else if (servicio.getSector() != null) sector = servicio.getSector().getNombre();

        String tamano = perfil != null ? perfil.getTamano() : "PEQUENA";
        boolean humano = tags(soa.getControl()).stream().anyMatch(t -> Set.of("personas", "capacitacion", "identidad", "acceso").contains(t));
        int fechaVencida = soa.getFechaObjetivo() != null
                && soa.getFechaObjetivo().isBefore(LocalDate.now())
                && !"IMPLEMENTADO".equals(soa.getEstadoImplementacion()) ? 1 : 0;

        int pendientes = (int) evidencias.stream().filter(e -> "PENDIENTE".equals(e.getEstado())).count();
        int rechazadas = (int) evidencias.stream().filter(e -> "RECHAZADA".equals(e.getEstado())).count();
        int vencidas = (int) evidencias.stream()
                .filter(e -> e.getFechaVencimiento() != null && e.getFechaVencimiento().isBefore(LocalDate.now()))
                .count();
        int recurrentes = (int) hallazgos.stream().filter(h -> Boolean.TRUE.equals(h.getRecurrente())).count();
        int severidadOrdinal = hallazgos.stream().mapToInt(this::severidadOrdinal).max().orElse(0);

        return new RpmMlFeatureRequest(
                analisis.getId(),
                sector,
                tamano,
                soa.getControl() != null ? soa.getControl().getDominio() : null,
                humano ? 1 : 0,
                soa.getAplicabilidad(),
                soa.getEstadoImplementacion(),
                soa.getPorcentajeImplementacion(),
                soa.getPuntajeRelevancia(),
                fechaVencida,
                riesgo != null ? riesgo.getProbabilidad() : null,
                riesgo != null ? riesgo.getImpacto() : null,
                riesgo != null ? riesgo.getNivelInherente() : null,
                riesgo != null ? RiesgoService.categoria(riesgo.getNivelInherente()) : null,
                evidencias.size(),
                pendientes,
                rechazadas,
                vencidas,
                hallazgos.size(),
                recurrentes,
                severidadOrdinal);
    }

    private int severidadOrdinal(HallazgoAuditoria h) {
        if (h == null || h.getSeveridad() == null) return 0;
        return switch (h.getSeveridad().toUpperCase(Locale.ROOT)) {
            case "CRITICA" -> 3;
            case "ALTA" -> 2;
            case "MEDIA" -> 1;
            default -> 0;
        };
    }

    private Set<String> tags(ControlCatalogo c) {
        if (c == null || c.getEtiquetas() == null) return Set.of();
        return Arrays.stream(c.getEtiquetas().split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
