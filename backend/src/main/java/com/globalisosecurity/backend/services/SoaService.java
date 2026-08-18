package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.SoaControlUpdateRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SoaService {
    private static final Set<String> APLICABILIDAD = Set.of("PENDIENTE","APLICABLE","NO_APLICABLE");
    private static final Set<String> ESTADOS = Set.of("NO_INICIADO","PLANIFICADO","PARCIAL","IMPLEMENTADO","NO_EFECTIVO");
    private final SoaControlRepository repository;
    private final ControlCatalogoRepository controlRepository;
    private final PerfilOrganizacionalRepository perfilRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final RiesgoControlRepository riesgoControlRepository;
    private final HallazgoAuditoriaRepository hallazgoRepository;
    private final ControlRelevanciaService relevanciaService;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public SoaService(SoaControlRepository repository, ControlCatalogoRepository controlRepository,
            PerfilOrganizacionalRepository perfilRepository,
            EvidenciaRepository evidenciaRepository, RiesgoControlRepository riesgoControlRepository,
            HallazgoAuditoriaRepository hallazgoRepository, ControlRelevanciaService relevanciaService,
            AccesoEmpresaService acceso, LogAuditoriaService logs) {
        this.repository=repository; this.controlRepository=controlRepository;
        this.perfilRepository=perfilRepository; this.evidenciaRepository=evidenciaRepository;
        this.riesgoControlRepository=riesgoControlRepository; this.hallazgoRepository=hallazgoRepository;
        this.relevanciaService=relevanciaService; this.acceso=acceso; this.logs=logs;
    }

    @Transactional
    public Map<String,Object> inicializar(Long servicioId) {
        Servicio servicio = acceso.servicioAutorizado(servicioId);
        PerfilOrganizacional perfil = perfilRepository.findByEmpresaId(servicio.getEmpresa().getId()).orElse(null);
        int creados=0, actualizados=0;
        for (ControlCatalogo control : controlRepository.findByActivoTrueOrderByCodigoAsc()) {
            Optional<SoaControl> existente = repository.findByServicioIdAndControlId(servicioId, control.getId());
            ControlRelevanciaService.Relevancia r = relevanciaService.evaluar(control, servicio, perfil);
            if (existente.isPresent()) {
                SoaControl s=existente.get(); s.setPuntajeRelevancia(r.puntaje()); s.setRecomendacionContextual(r.motivo());
                s.setActualizadoEn(LocalDateTime.now()); repository.save(s); actualizados++;
            } else {
                SoaControl s=new SoaControl(); s.setServicio(servicio); s.setControl(control);
                s.setPuntajeRelevancia(r.puntaje()); s.setRecomendacionContextual(r.motivo()); repository.save(s); creados++;
            }
        }
        logs.registrarLog("INICIALIZAR", "SOA", "SoA de servicio " + servicioId + ": " + creados + " controles creados");
        return Map.of("servicioId",servicioId,"creados",creados,"actualizados",actualizados,"total",repository.countByServicioId(servicioId));
    }

    public List<Map<String,Object>> listar(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        long expected = controlRepository.findByActivoTrueOrderByCodigoAsc().size();
        long current = repository.countByServicioIdAndControlActivoTrue(servicioId);
        if (current < expected) inicializar(servicioId);
        return repository.findByServicioIdAndControlActivoTrueOrderByControlCodigoAsc(servicioId).stream().map(this::mapear).toList();
    }

    public Map<String,Object> obtener(Long id) {
        SoaControl s=repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Registro SoA no encontrado"));
        acceso.servicioAutorizado(s.getServicio().getId()); return mapear(s);
    }

    @Transactional
    public Map<String,Object> actualizar(Long id, SoaControlUpdateRequest r) {
        SoaControl s=repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Registro SoA no encontrado"));
        acceso.servicioAutorizado(s.getServicio().getId());
        if (r==null) throw new BadRequestException("El cuerpo de la solicitud es obligatorio");
        String a=normalizar(r.aplicabilidad(),s.getAplicabilidad());
        String e=normalizar(r.estadoImplementacion(),s.getEstadoImplementacion());
        if(!APLICABILIDAD.contains(a)) throw new BadRequestException("Aplicabilidad no válida");
        if(!ESTADOS.contains(e)) throw new BadRequestException("Estado de implementación no válido");
        if("NO_APLICABLE".equals(a) && (r.justificacionAplicabilidad()==null || r.justificacionAplicabilidad().isBlank()))
            throw new BadRequestException("La justificación es obligatoria para declarar un control no aplicable");
        int p=r.porcentajeImplementacion()==null ? s.getPorcentajeImplementacion() : Math.max(0,Math.min(100,r.porcentajeImplementacion()));
        if ("NO_APLICABLE".equals(a)) {
            e = "NO_INICIADO";
            p = 0;
        } else {
            if ("IMPLEMENTADO".equals(e) && p < 100) p = 100;
            if ("NO_INICIADO".equals(e) && p > 0) e = "PARCIAL";
        }
        s.setAplicabilidad(a); s.setEstadoImplementacion(e); s.setPorcentajeImplementacion(p);
        s.setJustificacionAplicabilidad(trim(r.justificacionAplicabilidad())); s.setResponsable(trim(r.responsable()));
        s.setFechaObjetivo(r.fechaObjetivo()); s.setObservaciones(trim(r.observaciones())); s.setActualizadoEn(LocalDateTime.now());
        repository.save(s);
        logs.registrarLog("ACTUALIZAR", "SOA", "Se actualizó " + s.getControl().getCodigo() + " del servicio " + s.getServicio().getId());
        return mapear(s);
    }

    public Map<String,Object> resumen(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        List<SoaControl> items=repository.findByServicioIdAndControlActivoTrueOrderByControlCodigoAsc(servicioId);
        long total=items.size(), aplicables=items.stream().filter(x->"APLICABLE".equals(x.getAplicabilidad())).count();
        long noAplicables=items.stream().filter(x->"NO_APLICABLE".equals(x.getAplicabilidad())).count();
        long pendientes=total-aplicables-noAplicables;
        long implementados=items.stream().filter(x->"IMPLEMENTADO".equals(x.getEstadoImplementacion())).count();
        long parciales=items.stream().filter(x->"PARCIAL".equals(x.getEstadoImplementacion())||"PLANIFICADO".equals(x.getEstadoImplementacion())).count();
        int porcentaje=aplicables==0?0:(int)Math.round(items.stream().filter(x->"APLICABLE".equals(x.getAplicabilidad())).mapToInt(SoaControl::getPorcentajeImplementacion).average().orElse(0));
        return new LinkedHashMap<>(Map.of("total",total,"aplicables",aplicables,"noAplicables",noAplicables,"pendientes",pendientes,"implementados",implementados,"parciales",parciales,"porcentaje",porcentaje));
    }

    private Map<String,Object> mapear(SoaControl s){
        List<Evidencia> evidencias=evidenciaRepository.findBySoaControlIdOrderByFechaCargaDesc(s.getId());
        List<RiesgoControl> relaciones=riesgoControlRepository.findByControlIdAndRiesgoServicioId(s.getControl().getId(),s.getServicio().getId());
        long hallazgos=hallazgoRepository.findBySoaControlIdAndEstadoNot(s.getId(),"CERRADO").size();
        Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",s.getId()); m.put("servicioId",s.getServicio().getId()); m.put("empresaId",s.getServicio().getEmpresa().getId());
        m.put("control",Map.of("id",s.getControl().getId(),"codigo",s.getControl().getCodigo(),"dominio",s.getControl().getDominio(),"titulo",s.getControl().getTitulo(),"descripcion",safe(s.getControl().getDescripcion()),"pregunta",safe(s.getControl().getPreguntaEvaluacion())));
        m.put("aplicabilidad",s.getAplicabilidad()); m.put("justificacionAplicabilidad",safe(s.getJustificacionAplicabilidad()));
        m.put("estadoImplementacion",s.getEstadoImplementacion()); m.put("porcentajeImplementacion",s.getPorcentajeImplementacion());
        m.put("responsable",safe(s.getResponsable())); m.put("fechaObjetivo",s.getFechaObjetivo()); m.put("observaciones",safe(s.getObservaciones()));
        m.put("puntajeRelevancia",s.getPuntajeRelevancia()); m.put("nivelRelevancia",s.getPuntajeRelevancia()>=60?"ALTA":s.getPuntajeRelevancia()>=35?"MEDIA":"BASE");
        m.put("recomendacionContextual",safe(s.getRecomendacionContextual()));
        m.put("evidencias",Map.of("total",evidencias.size(),"validadas",evidencias.stream().filter(x->"VALIDADA".equals(x.getEstado())).count(),"pendientes",evidencias.stream().filter(x->"PENDIENTE".equals(x.getEstado())).count(),"rechazadas",evidencias.stream().filter(x->"RECHAZADA".equals(x.getEstado())).count()));
        m.put("riesgos",relaciones.stream().map(rc->Map.of(
                "id",rc.getRiesgo().getId(),
                "codigo",rc.getRiesgo().getCodigo(),
                "nombre",rc.getRiesgo().getNombre(),
                "nivel",rc.getRiesgo().getNivelInherente(),
                "categoria",RiesgoService.categoria(rc.getRiesgo().getNivelInherente()))).toList());
        m.put("hallazgosAbiertos",hallazgos); m.put("vencido",s.getFechaObjetivo()!=null && s.getFechaObjetivo().isBefore(LocalDate.now()) && !"IMPLEMENTADO".equals(s.getEstadoImplementacion()));
        return m;
    }
    private String normalizar(String nuevo,String actual){return nuevo==null||nuevo.isBlank()?actual:nuevo.trim().toUpperCase();}
    private String trim(String v){return v==null?null:v.trim();}
    private String safe(String v){return v==null?"":v;}
}
