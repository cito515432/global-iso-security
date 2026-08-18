package com.globalisosecurity.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalisosecurity.backend.dto.*;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FormacionService {
    private static final Set<String> RESPUESTAS_VALIDAS = Set.of("A", "B", "C", "D");

    private final CapacitacionRepository capacitacionRepository;
    private final ModuloCapacitacionRepository moduloRepository;
    private final ParticipanteCapacitacionRepository participanteRepository;
    private final PreguntaCapacitacionRepository preguntaRepository;
    private final IntentoCapacitacionRepository intentoRepository;
    private final ConstanciaCapacitacionRepository constanciaRepository;
    private final ConstanciaCapacitacionService constanciaService;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;
    private final ObjectMapper objectMapper;

    public FormacionService(CapacitacionRepository capacitacionRepository,
            ModuloCapacitacionRepository moduloRepository,
            ParticipanteCapacitacionRepository participanteRepository,
            PreguntaCapacitacionRepository preguntaRepository,
            IntentoCapacitacionRepository intentoRepository,
            ConstanciaCapacitacionRepository constanciaRepository,
            ConstanciaCapacitacionService constanciaService,
            AccesoEmpresaService acceso, LogAuditoriaService logs, ObjectMapper objectMapper) {
        this.capacitacionRepository = capacitacionRepository;
        this.moduloRepository = moduloRepository;
        this.participanteRepository = participanteRepository;
        this.preguntaRepository = preguntaRepository;
        this.intentoRepository = intentoRepository;
        this.constanciaRepository = constanciaRepository;
        this.constanciaService = constanciaService;
        this.acceso = acceso;
        this.logs = logs;
        this.objectMapper = objectMapper;
    }

    public Map<String,Object> dashboard(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        List<Capacitacion> caps = capacitacionRepository.findByServicioId(servicioId);
        List<ParticipanteCapacitacion> parts = participanteRepository.findByCapacitacionServicioId(servicioId);
        double progreso = parts.stream().mapToInt(p -> valor(p.getProgresoPorcentaje())).average().orElse(0);
        double puntaje = parts.stream().filter(p -> p.getPuntajeEvaluacion() != null)
                .mapToDouble(ParticipanteCapacitacion::getPuntajeEvaluacion).average().orElse(0);
        long aprobados = parts.stream().filter(this::aprobado).count();
        long pendientes = parts.stream().filter(p -> !aprobado(p)).count();
        Map<String,Object> r = new LinkedHashMap<>();
        r.put("programas", caps.size());
        r.put("activos", caps.stream().filter(c -> !"COMPLETADA".equals(c.getEstado())).count());
        r.put("participantes", parts.size());
        r.put("progresoPromedio", Math.round(progreso));
        r.put("puntajePromedio", Math.round(puntaje));
        r.put("aprobados", aprobados);
        r.put("pendientes", pendientes);
        r.put("rpm", caps.stream().filter(c -> Boolean.TRUE.equals(c.getCreadaPorRpm())).count());
        r.put("constancias", constanciaRepository.findByServicioId(servicioId).size());
        return r;
    }

    public List<Map<String,Object>> listar(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return capacitacionRepository.findByServicioId(servicioId).stream().map(this::mapCap).toList();
    }

    public Map<String,Object> detalle(Long id) {
        Capacitacion c = cap(id); acceso.servicioAutorizado(c.getServicio().getId()); return mapCap(c);
    }

    @Transactional
    public ModuloCapacitacion guardarModulo(Long capacitacionId, Long moduloId, ModuloCapacitacionRequest q) {
        Capacitacion c = cap(capacitacionId); acceso.servicioAutorizado(c.getServicio().getId());
        if (q == null || q.titulo() == null || q.titulo().isBlank()) throw new BadRequestException("El título del módulo es obligatorio");
        ModuloCapacitacion m = moduloId == null ? new ModuloCapacitacion() : moduloRepository.findById(moduloId)
                .orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado"));
        if (moduloId != null && !m.getCapacitacion().getId().equals(capacitacionId)) throw new BadRequestException("El módulo no pertenece a la capacitación");
        m.setCapacitacion(c); m.setTitulo(q.titulo().trim()); m.setDescripcion(trim(q.descripcion()));
        m.setContenido(q.contenido()); m.setMaterialUrl(trim(q.materialUrl())); m.setVideoUrl(trim(q.videoUrl()));
        m.setOrden(q.orden() == null ? 1 : Math.max(1, q.orden()));
        m.setDuracionMinutos(q.duracionMinutos() == null ? 15 : Math.max(1, q.duracionMinutos()));
        m.setObligatorio(q.obligatorio() == null || q.obligatorio());
        ModuloCapacitacion saved = moduloRepository.save(m);
        logs.registrarLog(moduloId == null ? "CREAR" : "ACTUALIZAR", "MODULOS_CAPACITACION", "Módulo " + saved.getId() + " de capacitación " + capacitacionId);
        return saved;
    }

    @Transactional
    public void eliminarModulo(Long id) {
        ModuloCapacitacion m = moduloRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Módulo no encontrado"));
        acceso.servicioAutorizado(m.getCapacitacion().getServicio().getId()); moduloRepository.delete(m);
        logs.registrarLog("ELIMINAR", "MODULOS_CAPACITACION", "Se eliminó el módulo " + id);
    }

    @Transactional
    public ParticipanteCapacitacion guardarParticipante(Long capacitacionId, Long participanteId, ParticipanteCapacitacionRequest q) {
        Capacitacion c = cap(capacitacionId); acceso.servicioAutorizado(c.getServicio().getId());
        if (q == null || q.nombre() == null || q.nombre().isBlank() || q.email() == null || q.email().isBlank()) {
            throw new BadRequestException("Nombre y email son obligatorios");
        }
        ParticipanteCapacitacion p = participanteId == null
                ? participanteRepository.findByCapacitacionIdAndEmail(capacitacionId, q.email().trim().toLowerCase()).orElse(new ParticipanteCapacitacion())
                : participanteRepository.findById(participanteId).orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        if (participanteId != null && !p.getCapacitacion().getId().equals(capacitacionId)) throw new BadRequestException("El participante no pertenece a la capacitación");
        p.setCapacitacion(c); p.setNombre(q.nombre().trim()); p.setEmail(q.email().trim().toLowerCase());
        p.setDocumento(trim(q.documento())); p.setCargo(trim(q.cargo()));
        if (q.estado() != null && !q.estado().isBlank()) p.setEstado(q.estado().trim().toUpperCase());
        int progreso = q.progresoPorcentaje() == null ? valor(p.getProgresoPorcentaje()) : Math.max(0, Math.min(100, q.progresoPorcentaje()));
        p.setProgresoPorcentaje(progreso);
        if (q.puntajeEvaluacion() != null) p.setPuntajeEvaluacion(Math.max(0, Math.min(100, q.puntajeEvaluacion())));
        if (progreso >= 100) {
            p.setEstado("COMPLETADO");
            if (p.getFechaFinalizacion() == null) p.setFechaFinalizacion(LocalDateTime.now());
        }
        ParticipanteCapacitacion saved = participanteRepository.save(p);
        emitirSiAplica(saved);
        logs.registrarLog(participanteId == null ? "ASIGNAR" : "ACTUALIZAR", "PARTICIPANTES_CAPACITACION", "Participante " + saved.getId() + " en capacitación " + capacitacionId);
        return saved;
    }

    @Transactional
    public void eliminarParticipante(Long id) {
        ParticipanteCapacitacion p = participanteRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        acceso.servicioAutorizado(p.getCapacitacion().getServicio().getId());
        if (constanciaRepository.existsByParticipanteId(id)) throw new BadRequestException("No se puede eliminar un participante con constancia emitida");
        participanteRepository.delete(p); logs.registrarLog("ELIMINAR", "PARTICIPANTES_CAPACITACION", "Se eliminó el participante " + id);
    }

    public List<PreguntaCapacitacion> listarPreguntas(Long capacitacionId) {
        Capacitacion c = cap(capacitacionId); acceso.servicioAutorizado(c.getServicio().getId());
        return preguntaRepository.findByCapacitacionIdOrderByOrdenAsc(capacitacionId);
    }

    @Transactional
    public PreguntaCapacitacion guardarPregunta(Long capacitacionId, Long preguntaId, PreguntaCapacitacionRequest q) {
        Capacitacion c = cap(capacitacionId); acceso.servicioAutorizado(c.getServicio().getId()); validarPregunta(q);
        PreguntaCapacitacion p = preguntaId == null ? new PreguntaCapacitacion() : preguntaRepository.findById(preguntaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada"));
        if (preguntaId != null && !p.getCapacitacion().getId().equals(capacitacionId)) throw new BadRequestException("La pregunta no pertenece a la capacitación");
        String correcta = q.respuestaCorrecta().trim().toUpperCase();
        if (!RESPUESTAS_VALIDAS.contains(correcta)) throw new BadRequestException("La respuesta correcta debe ser A, B, C o D");
        if (("C".equals(correcta) && vacio(q.opcionC())) || ("D".equals(correcta) && vacio(q.opcionD()))) throw new BadRequestException("La opción correcta seleccionada no tiene contenido");
        p.setCapacitacion(c); p.setEnunciado(q.enunciado().trim()); p.setOpcionA(q.opcionA().trim()); p.setOpcionB(q.opcionB().trim());
        p.setOpcionC(trim(q.opcionC())); p.setOpcionD(trim(q.opcionD())); p.setRespuestaCorrecta(correcta);
        p.setExplicacion(trim(q.explicacion())); p.setPuntos(q.puntos() == null ? 1 : Math.max(1, q.puntos()));
        p.setOrden(q.orden() == null ? 1 : Math.max(1, q.orden())); p.setActiva(q.activa() == null || q.activa());
        PreguntaCapacitacion saved = preguntaRepository.save(p);
        logs.registrarLog(preguntaId == null ? "CREAR" : "ACTUALIZAR", "PREGUNTAS_CAPACITACION", "Pregunta " + saved.getId() + " de capacitación " + capacitacionId);
        return saved;
    }

    @Transactional
    public void eliminarPregunta(Long id) {
        PreguntaCapacitacion p = preguntaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada"));
        acceso.servicioAutorizado(p.getCapacitacion().getServicio().getId()); preguntaRepository.delete(p);
        logs.registrarLog("ELIMINAR", "PREGUNTAS_CAPACITACION", "Se eliminó la pregunta " + id);
    }

    @Transactional
    public Map<String,Object> evaluar(Long capacitacionId, Long participanteId, IntentoCapacitacionRequest req) {
        Capacitacion c = cap(capacitacionId); acceso.servicioAutorizado(c.getServicio().getId());
        ParticipanteCapacitacion participante = participanteRepository.findById(participanteId)
                .orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        if (!participante.getCapacitacion().getId().equals(capacitacionId)) throw new BadRequestException("El participante no pertenece a la capacitación");
        List<PreguntaCapacitacion> preguntas = preguntaRepository.findByCapacitacionIdAndActivaTrueOrderByOrdenAsc(capacitacionId);
        if (preguntas.isEmpty()) throw new BadRequestException("La capacitación no tiene preguntas activas");
        Map<Long,String> respuestas = req == null || req.respuestas() == null ? Map.of() : req.respuestas();
        int totalPuntos = preguntas.stream().mapToInt(p -> Math.max(1, p.getPuntos())).sum();
        int ganados = 0; int correctas = 0;
        Map<String,String> normalizadas = new LinkedHashMap<>();
        for (PreguntaCapacitacion p : preguntas) {
            String r = respuestas.get(p.getId()); r = r == null ? "" : r.trim().toUpperCase(); normalizadas.put(String.valueOf(p.getId()), r);
            if (p.getRespuestaCorrecta().equals(r)) { correctas++; ganados += Math.max(1, p.getPuntos()); }
        }
        double puntaje = Math.round((ganados * 10000.0 / totalPuntos)) / 100.0;
        boolean aprobado = puntaje >= c.getPuntajeMinimo();
        IntentoCapacitacion intento = new IntentoCapacitacion(); intento.setParticipante(participante); intento.setPuntaje(puntaje);
        intento.setAprobado(aprobado); intento.setRespuestasCorrectas(correctas); intento.setTotalPreguntas(preguntas.size());
        try { intento.setRespuestasJson(objectMapper.writeValueAsString(normalizadas)); }
        catch (JsonProcessingException ex) { throw new BadRequestException("No fue posible guardar las respuestas"); }
        intento = intentoRepository.save(intento);
        participante.setIntentos(valor(participante.getIntentos()) + 1); participante.setPuntajeEvaluacion(puntaje);
        if (aprobado) { participante.setProgresoPorcentaje(100); participante.setEstado("COMPLETADO"); if (participante.getFechaFinalizacion() == null) participante.setFechaFinalizacion(LocalDateTime.now()); }
        participanteRepository.save(participante); emitirSiAplica(participante);
        logs.registrarLog("EVALUAR", "EVALUACION_CAPACITACION", "Intento " + intento.getId() + " con puntaje " + puntaje + " para participante " + participanteId);
        return mapIntento(intento);
    }

    public List<Map<String,Object>> intentos(Long participanteId) {
        ParticipanteCapacitacion p = participanteRepository.findById(participanteId).orElseThrow(() -> new ResourceNotFoundException("Participante no encontrado"));
        acceso.servicioAutorizado(p.getCapacitacion().getServicio().getId());
        return intentoRepository.findByParticipanteIdOrderByFechaIntentoDesc(participanteId).stream().map(this::mapIntento).toList();
    }

    @Transactional
    public Capacitacion crearDesdeRpm(Servicio s, String controlCodigo, Long riesgoId, String motivo, String accion) {
        Capacitacion c = new Capacitacion(); c.setServicio(s); c.setTitulo("Plan formativo RPM - " + (controlCodigo == null ? "Seguridad" : controlCodigo));
        c.setDescripcion(accion); c.setObjetivo("Reducir la señal de peligro de origen humano identificada por el motor RPM.");
        c.setEstado("PENDIENTE"); c.setPuntajeMinimo(80); c.setPublicoObjetivo("Personal relacionado con el control o riesgo analizado");
        c.setCreadaPorRpm(true); c.setMotivoRpm(motivo); c.setControlCodigo(controlCodigo); c.setRiesgoIdReferencia(riesgoId); c = capacitacionRepository.save(c);
        ModuloCapacitacion m = new ModuloCapacitacion(); m.setCapacitacion(c); m.setTitulo("Contenido por definir y validar");
        m.setDescripcion("El capacitador debe adaptar el contenido al contexto de la organización y a la recomendación RPM.");
        m.setContenido("Esta propuesta fue creada automáticamente como respuesta sugerida. Requiere revisión humana antes de asignarse.");
        m.setOrden(1); moduloRepository.save(m); logs.registrarLog("CREAR", "CAPACITACION_RPM", "Se creó capacitación desde recomendación RPM para " + controlCodigo); return c;
    }

    private void emitirSiAplica(ParticipanteCapacitacion p) {
        if (p.getId() != null && aprobado(p) && valor(p.getProgresoPorcentaje()) >= 100 && !vacio(p.getDocumento()) && !constanciaRepository.existsByParticipanteId(p.getId())) {
            constanciaService.emitirParaParticipante(p);
        }
    }

    private boolean aprobado(ParticipanteCapacitacion p) {
        return p.getPuntajeEvaluacion() != null && p.getCapacitacion() != null && p.getPuntajeEvaluacion() >= p.getCapacitacion().getPuntajeMinimo();
    }

    private Capacitacion cap(Long id) { return capacitacionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Capacitación no encontrada")); }

    private Map<String,Object> mapCap(Capacitacion c) {
        List<ModuloCapacitacion> ms = moduloRepository.findByCapacitacionIdOrderByOrdenAsc(c.getId());
        List<ParticipanteCapacitacion> ps = participanteRepository.findByCapacitacionIdOrderByNombreAsc(c.getId());
        List<PreguntaCapacitacion> qs = preguntaRepository.findByCapacitacionIdOrderByOrdenAsc(c.getId());
        Map<String,Object> m = new LinkedHashMap<>();
        m.put("id", c.getId()); m.put("servicioId", c.getServicio().getId()); m.put("empresa", c.getServicio().getEmpresa().getNombre());
        m.put("titulo", c.getTitulo()); m.put("descripcion", safe(c.getDescripcion())); m.put("objetivo", safe(c.getObjetivo())); m.put("estado", c.getEstado());
        m.put("fechaInicio", c.getFechaInicio()); m.put("fechaLimite", c.getFechaLimite()); m.put("puntajeMinimo", c.getPuntajeMinimo());
        m.put("publicoObjetivo", safe(c.getPublicoObjetivo())); m.put("materialUrl", safe(c.getMaterialUrl())); m.put("videoUrl", safe(c.getVideoUrl()));
        m.put("creadaPorRpm", c.getCreadaPorRpm()); m.put("motivoRpm", safe(c.getMotivoRpm())); m.put("controlCodigo", safe(c.getControlCodigo()));
        m.put("riesgoId", c.getRiesgoIdReferencia()); m.put("modulos", ms); m.put("participantes", ps); m.put("preguntasTotal", qs.size());
        m.put("preguntasActivas", qs.stream().filter(q -> Boolean.TRUE.equals(q.getActiva())).count());
        m.put("constancias", constanciaRepository.findByCapacitacionId(c.getId()).size());
        m.put("progresoPromedio", ps.isEmpty() ? 0 : Math.round(ps.stream().mapToInt(p -> valor(p.getProgresoPorcentaje())).average().orElse(0)));
        m.put("puntajePromedio", ps.stream().filter(p -> p.getPuntajeEvaluacion() != null).mapToDouble(ParticipanteCapacitacion::getPuntajeEvaluacion).average().orElse(0));
        m.put("aprobados", ps.stream().filter(this::aprobado).count());
        return m;
    }

    private Map<String,Object> mapIntento(IntentoCapacitacion i) {
        Map<String,Object> m = new LinkedHashMap<>(); m.put("id", i.getId()); m.put("participanteId", i.getParticipante().getId());
        m.put("participante", i.getParticipante().getNombre()); m.put("fecha", i.getFechaIntento()); m.put("puntaje", i.getPuntaje());
        m.put("aprobado", i.getAprobado()); m.put("correctas", i.getRespuestasCorrectas()); m.put("total", i.getTotalPreguntas());
        return m;
    }

    private void validarPregunta(PreguntaCapacitacionRequest q) {
        if (q == null || vacio(q.enunciado()) || vacio(q.opcionA()) || vacio(q.opcionB()) || vacio(q.respuestaCorrecta())) {
            throw new BadRequestException("Enunciado, opciones A/B y respuesta correcta son obligatorios");
        }
    }
    private int valor(Integer v) { return v == null ? 0 : v; }
    private String trim(String s) { return s == null ? null : s.trim(); }
    private String safe(String s) { return s == null ? "" : s; }
    private boolean vacio(String s) { return s == null || s.isBlank(); }
}
