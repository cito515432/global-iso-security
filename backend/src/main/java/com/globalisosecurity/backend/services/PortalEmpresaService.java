package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.PortalEmpresaDTO;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Capacitacion;
import com.globalisosecurity.backend.models.Evidencia;
import com.globalisosecurity.backend.models.HallazgoAuditoria;
import com.globalisosecurity.backend.models.ParticipanteCapacitacion;
import com.globalisosecurity.backend.models.PerfilOrganizacional;
import com.globalisosecurity.backend.models.Riesgo;
import com.globalisosecurity.backend.models.RpmAnalisis;
import com.globalisosecurity.backend.models.RpmDecision;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.models.SoaControl;
import com.globalisosecurity.backend.repositories.CapacitacionRepository;
import com.globalisosecurity.backend.repositories.EvidenciaRepository;
import com.globalisosecurity.backend.repositories.FirmaRepository;
import com.globalisosecurity.backend.repositories.HallazgoAuditoriaRepository;
import com.globalisosecurity.backend.repositories.ParticipanteCapacitacionRepository;
import com.globalisosecurity.backend.repositories.PerfilOrganizacionalRepository;
import com.globalisosecurity.backend.repositories.RiesgoControlRepository;
import com.globalisosecurity.backend.repositories.RiesgoRepository;
import com.globalisosecurity.backend.repositories.RpmAnalisisRepository;
import com.globalisosecurity.backend.repositories.RpmDecisionRepository;
import com.globalisosecurity.backend.repositories.RpmMemoriaRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import com.globalisosecurity.backend.repositories.SoaControlRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PortalEmpresaService {
    private final AccesoEmpresaService acceso;
    private final ServicioRepository servicioRepository;
    private final PerfilOrganizacionalRepository perfilRepository;
    private final SoaControlRepository soaRepository;
    private final RiesgoRepository riesgoRepository;
    private final RiesgoControlRepository riesgoControlRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final HallazgoAuditoriaRepository hallazgoRepository;
    private final RpmAnalisisRepository rpmRepository;
    private final RpmDecisionRepository decisionRepository;
    private final RpmMemoriaRepository memoriaRepository;
    private final CapacitacionRepository capacitacionRepository;
    private final ParticipanteCapacitacionRepository participanteRepository;
    private final FirmaRepository firmaRepository;
    private final SoaService soaService;

    public PortalEmpresaService(
            AccesoEmpresaService acceso,
            ServicioRepository servicioRepository,
            PerfilOrganizacionalRepository perfilRepository,
            SoaControlRepository soaRepository,
            RiesgoRepository riesgoRepository,
            RiesgoControlRepository riesgoControlRepository,
            EvidenciaRepository evidenciaRepository,
            HallazgoAuditoriaRepository hallazgoRepository,
            RpmAnalisisRepository rpmRepository,
            RpmDecisionRepository decisionRepository,
            RpmMemoriaRepository memoriaRepository,
            CapacitacionRepository capacitacionRepository,
            ParticipanteCapacitacionRepository participanteRepository,
            FirmaRepository firmaRepository,
            SoaService soaService) {
        this.acceso = acceso;
        this.servicioRepository = servicioRepository;
        this.perfilRepository = perfilRepository;
        this.soaRepository = soaRepository;
        this.riesgoRepository = riesgoRepository;
        this.riesgoControlRepository = riesgoControlRepository;
        this.evidenciaRepository = evidenciaRepository;
        this.hallazgoRepository = hallazgoRepository;
        this.rpmRepository = rpmRepository;
        this.decisionRepository = decisionRepository;
        this.memoriaRepository = memoriaRepository;
        this.capacitacionRepository = capacitacionRepository;
        this.participanteRepository = participanteRepository;
        this.firmaRepository = firmaRepository;
        this.soaService = soaService;
    }

    public PortalEmpresaDTO miResumen() {
        return resumen(acceso.empresaActualObligatoria());
    }

    public PortalEmpresaDTO resumen(Long empresaId) {
        acceso.validarEmpresa(empresaId);
        Servicio servicio = servicioRepository.findFirstByEmpresaIdOrderByFechaCreacionDesc(empresaId)
                .orElseThrow(() -> new ResourceNotFoundException("La empresa no tiene un servicio activo"));
        if (soaRepository.countByServicioId(servicio.getId()) == 0) {
            soaService.inicializar(servicio.getId());
        }

        PerfilOrganizacional perfil = perfilRepository.findByEmpresaId(empresaId).orElse(null);
        List<SoaControl> soa = soaRepository.findByServicioIdOrderByControlCodigoAsc(servicio.getId());
        List<Riesgo> riesgos = riesgoRepository.findByServicioIdOrderByNivelInherenteDesc(servicio.getId());
        List<Evidencia> evidencias = evidenciaRepository.findByServicioIdOrderByFechaCargaDesc(servicio.getId());
        List<HallazgoAuditoria> hallazgos = hallazgoRepository.findByServicioIdOrderByFechaDeteccionDesc(servicio.getId());
        List<RpmAnalisis> rpm = ultimosAnalisis(servicio.getId());
        List<Capacitacion> capacitaciones = capacitacionRepository.findByServicioId(servicio.getId());
        List<ParticipanteCapacitacion> participantes = participanteRepository.findByCapacitacionServicioId(servicio.getId());

        long aplicables = soa.stream().filter(x -> "APLICABLE".equals(x.getAplicabilidad())).count();
        long noAplicables = soa.stream().filter(x -> "NO_APLICABLE".equals(x.getAplicabilidad())).count();
        long pendientes = soa.size() - aplicables - noAplicables;
        long implementados = soa.stream().filter(x -> "IMPLEMENTADO".equals(x.getEstadoImplementacion())).count();
        long parciales = soa.stream().filter(x -> Set.of("PARCIAL", "PLANIFICADO").contains(x.getEstadoImplementacion())).count();
        long noIniciados = soa.stream().filter(x -> "NO_INICIADO".equals(x.getEstadoImplementacion())).count();
        int implementacion = aplicables == 0 ? 0 : (int) Math.round(
                soa.stream().filter(x -> "APLICABLE".equals(x.getAplicabilidad()))
                        .mapToInt(SoaControl::getPorcentajeImplementacion).average().orElse(0));
        int decisionSoa = soa.isEmpty() ? 0 : (int) Math.round((aplicables + noAplicables) * 100.0 / soa.size());

        long criticos = riesgos.stream().filter(x -> "CRITICO".equals(RiesgoService.categoria(x.getNivelInherente()))).count();
        long altos = riesgos.stream().filter(x -> "ALTO".equals(RiesgoService.categoria(x.getNivelInherente()))).count();
        long medios = riesgos.stream().filter(x -> "MEDIO".equals(RiesgoService.categoria(x.getNivelInherente()))).count();
        long bajos = riesgos.size() - criticos - altos - medios;
        long riesgosAbiertos = riesgos.stream().filter(x -> !"CERRADO".equals(x.getEstado())).count();

        long validadas = evidencias.stream().filter(x -> "VALIDADA".equals(x.getEstado())).count();
        long evidenciasPendientes = evidencias.stream().filter(x -> "PENDIENTE".equals(x.getEstado())).count();
        long rechazadas = evidencias.stream().filter(x -> "RECHAZADA".equals(x.getEstado())).count();
        long vencidas = evidencias.stream().filter(x -> x.getFechaVencimiento() != null
                && x.getFechaVencimiento().isBefore(LocalDate.now())).count();

        long hallazgosAbiertos = hallazgos.stream().filter(x -> !"CERRADO".equals(x.getEstado())).count();
        long recurrentes = hallazgos.stream().filter(x -> Boolean.TRUE.equals(x.getRecurrente())
                && !"CERRADO".equals(x.getEstado())).count();
        long hallazgosCriticos = hallazgos.stream().filter(x -> "CRITICA".equals(x.getSeveridad())
                && !"CERRADO".equals(x.getEstado())).count();

        long alertasRpm = rpm.stream().filter(x -> Set.of("ALTA", "CRITICA").contains(x.getPrioridad())
                && !"EVALUADO".equals(x.getEstado())).count();
        long decisionesPendientes = rpm.stream()
                .flatMap(x -> decisionRepository.findByAnalisisId(x.getId()).stream())
                .filter(x -> "PENDIENTE".equals(x.getEstado()))
                .count();
        long memorias = memoriaRepository.countByAnalisisServicioId(servicio.getId());

        int finalizacionFormacion = participantes.isEmpty() ? 0 : (int) Math.round(
                participantes.stream().mapToInt(ParticipanteCapacitacion::getProgresoPorcentaje).average().orElse(0));
        int aprobacionFormacion = participantes.isEmpty() ? 0 : (int) Math.round(
                participantes.stream().filter(x -> x.getPuntajeEvaluacion() != null)
                        .mapToDouble(x -> x.getPuntajeEvaluacion() >= x.getCapacitacion().getPuntajeMinimo() ? 100 : 0)
                        .average().orElse(0));

        int contexto = puntajeContexto(perfil);
        int etapaRiesgos = puntajeRiesgos(riesgos);
        int auditoria = evidencias.isEmpty() ? 0 : (int) Math.min(100,
                Math.round(validadas * 100.0 / evidencias.size()));
        int cierre = Set.of("FIRMADO", "CERRADO", "FINALIZADO").contains(servicio.getEstado())
                ? 100 : firmaRepository.findByServicioId(servicio.getId()).isEmpty() ? 0 : 50;
        int general = (int) Math.round(contexto * .10 + etapaRiesgos * .15 + decisionSoa * .20
                + implementacion * .30 + auditoria * .15 + cierre * .10);

        List<PortalEmpresaDTO.RpmItem> prioridades = rpm.stream()
                .filter(x -> Set.of("ALTA", "CRITICA").contains(x.getPrioridad()))
                .sorted(Comparator.comparingInt(RpmAnalisis::getPuntaje).reversed())
                .limit(10)
                .map(x -> new PortalEmpresaDTO.RpmItem(
                        x.getId(),
                        x.getSoaControl() != null ? x.getSoaControl().getControl().getCodigo() : "",
                        x.getSoaControl() != null ? x.getSoaControl().getControl().getTitulo() : "Análisis",
                        x.getPuntaje(), x.getPrioridad(), x.getResumen()))
                .toList();

        List<PortalEmpresaDTO.Actividad> actividades = new ArrayList<>();
        if (contexto < 100) actividades.add(new PortalEmpresaDTO.Actividad("CONTEXTO", "Completar contexto del SGSI",
                "Defina alcance, responsable, sector y parámetros organizacionales.", "MEDIA"));
        if (riesgos.isEmpty()) actividades.add(new PortalEmpresaDTO.Actividad("RIESGO", "Registrar riesgos",
                "La SoA debe relacionarse con riesgos identificados y valorados.", "ALTA"));
        else if (etapaRiesgos < 100) actividades.add(new PortalEmpresaDTO.Actividad("RIESGO", "Completar tratamiento de riesgos",
                "Relacione cada riesgo con controles y complete responsable, tratamiento y revisión.", "MEDIA"));
        if (pendientes > 0) actividades.add(new PortalEmpresaDTO.Actividad("SOA", "Definir aplicabilidad",
                "Hay " + pendientes + " controles pendientes de decisión.", "ALTA"));
        if (aplicables > 0 && evidencias.isEmpty()) actividades.add(new PortalEmpresaDTO.Actividad("EVIDENCIA", "Cargar evidencias",
                "No se han registrado evidencias para los controles aplicables.", "ALTA"));
        if (rechazadas > 0) actividades.add(new PortalEmpresaDTO.Actividad("EVIDENCIA", "Corregir evidencias",
                "Hay " + rechazadas + " evidencias rechazadas.", "ALTA"));
        if (hallazgosAbiertos > 0) actividades.add(new PortalEmpresaDTO.Actividad("AUDITORIA", "Tratar hallazgos",
                "Hay " + hallazgosAbiertos + " hallazgos abiertos.", hallazgosCriticos > 0 ? "CRITICA" : "MEDIA"));
        if (decisionesPendientes > 0) actividades.add(new PortalEmpresaDTO.Actividad("RPM", "Validar recomendaciones",
                "Hay " + decisionesPendientes + " decisiones RPM vigentes pendientes.", "ALTA"));
        if (!participantes.isEmpty() && finalizacionFormacion < 80) actividades.add(new PortalEmpresaDTO.Actividad("FORMACION", "Completar formación",
                "La finalización promedio es " + finalizacionFormacion + "%.", "MEDIA"));

        String sector = perfil != null && perfil.getSector() != null
                ? perfil.getSector().getNombre()
                : servicio.getSector() != null ? servicio.getSector().getNombre() : "";

        return new PortalEmpresaDTO(
                empresaId, servicio.getEmpresa().getNombre(), servicio.getId(), sector, servicio.getEstado(), general,
                new PortalEmpresaDTO.Etapas(contexto, etapaRiesgos, decisionSoa, implementacion, auditoria, cierre),
                new PortalEmpresaDTO.SoaResumen(soa.size(), aplicables, noAplicables, pendientes,
                        implementados, parciales, noIniciados, implementacion),
                new PortalEmpresaDTO.RiesgosResumen(riesgos.size(), criticos, altos, medios, bajos, riesgosAbiertos),
                new PortalEmpresaDTO.EvidenciasResumen(evidencias.size(), validadas, evidenciasPendientes, rechazadas, vencidas),
                new PortalEmpresaDTO.HallazgosResumen(hallazgosAbiertos, recurrentes, hallazgosCriticos),
                new PortalEmpresaDTO.RpmResumen(alertasRpm, decisionesPendientes, memorias),
                new PortalEmpresaDTO.CapacitacionResumen(capacitaciones.size(), participantes.size(),
                        finalizacionFormacion, aprobacionFormacion),
                prioridades, actividades);
    }

    private List<RpmAnalisis> ultimosAnalisis(Long servicioId) {
        Map<Long, RpmAnalisis> latest = new LinkedHashMap<>();
        for (RpmAnalisis analisis : rpmRepository.findByServicioIdOrderByGeneradoEnDesc(servicioId)) {
            Long key = analisis.getSoaControl() != null ? analisis.getSoaControl().getId() : -analisis.getId();
            latest.putIfAbsent(key, analisis);
        }
        return new ArrayList<>(latest.values());
    }

    private int puntajeContexto(PerfilOrganizacional perfil) {
        if (perfil == null) return 0;
        int score = 10;
        if (perfil.getSector() != null) score += 15;
        if (perfil.getTamano() != null && !perfil.getTamano().isBlank()) score += 10;
        if (perfil.getAlcanceSgsi() != null && !perfil.getAlcanceSgsi().isBlank()) score += 30;
        if (perfil.getResponsableSgsi() != null && !perfil.getResponsableSgsi().isBlank()) score += 25;
        if (perfil.getUmbralAceptacion() != null && perfil.getUmbralAceptacion() > 0) score += 10;
        return Math.min(100, score);
    }

    private int puntajeRiesgos(List<Riesgo> riesgos) {
        if (riesgos.isEmpty()) return 0;
        long relacionados = riesgos.stream().filter(r -> !riesgoControlRepository.findByRiesgoId(r.getId()).isEmpty()).count();
        long completos = riesgos.stream().filter(r -> noVacio(r.getResponsable()) && noVacio(r.getTratamiento())
                && r.getFechaRevision() != null).count();
        int score = 40;
        score += (int) Math.round(relacionados * 30.0 / riesgos.size());
        score += (int) Math.round(completos * 30.0 / riesgos.size());
        return Math.min(100, score);
    }

    private boolean noVacio(String value) {
        return value != null && !value.isBlank();
    }
}
