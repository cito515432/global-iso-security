package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CapacitacionService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("PENDIENTE", "EN_PROCESO", "COMPLETADA");

    private final CapacitacionRepository capacitacionRepository;
    private final ServicioRepository servicioRepository;
    private final ModuloCapacitacionRepository moduloRepository;
    private final ParticipanteCapacitacionRepository participanteRepository;
    private final PreguntaCapacitacionRepository preguntaRepository;
    private final IntentoCapacitacionRepository intentoRepository;
    private final ConstanciaCapacitacionRepository constanciaRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public CapacitacionService(CapacitacionRepository capacitacionRepository,
            ServicioRepository servicioRepository, ModuloCapacitacionRepository moduloRepository,
            ParticipanteCapacitacionRepository participanteRepository, PreguntaCapacitacionRepository preguntaRepository,
            IntentoCapacitacionRepository intentoRepository, ConstanciaCapacitacionRepository constanciaRepository,
            AccesoEmpresaService acceso, LogAuditoriaService logs) {
        this.capacitacionRepository = capacitacionRepository;
        this.servicioRepository = servicioRepository;
        this.moduloRepository = moduloRepository;
        this.participanteRepository = participanteRepository;
        this.preguntaRepository = preguntaRepository;
        this.intentoRepository = intentoRepository;
        this.constanciaRepository = constanciaRepository;
        this.acceso = acceso;
        this.logs = logs;
    }

    public List<Capacitacion> obtenerTodas() {
        Usuario usuario = acceso.usuarioActual();
        if (acceso.esAdministrador() || (usuario.getEmpresa() == null && acceso.esRolInternoGlobal())) {
            return capacitacionRepository.findAll();
        }
        if (usuario.getEmpresa() == null) return List.of();
        return capacitacionRepository.findByServicioEmpresaId(usuario.getEmpresa().getId());
    }

    public Optional<Capacitacion> obtenerPorId(Long id) {
        Optional<Capacitacion> resultado = capacitacionRepository.findById(id);
        resultado.ifPresent(c -> acceso.servicioAutorizado(c.getServicio().getId()));
        return resultado;
    }

    public List<Capacitacion> obtenerPorEstado(String estado) {
        String normalizado = normalizarEstado(estado);
        return obtenerTodas().stream().filter(c -> normalizado.equals(c.getEstado())).toList();
    }

    public List<Capacitacion> obtenerPorServicio(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return capacitacionRepository.findByServicioId(servicioId);
    }

    public List<Capacitacion> obtenerPorEmpresa(Long empresaId) {
        acceso.validarEmpresa(empresaId);
        return capacitacionRepository.findByServicioEmpresaId(empresaId);
    }

    @Transactional
    public Capacitacion crearCapacitacion(Capacitacion capacitacion) {
        validarCapacitacion(capacitacion);
        String estado = normalizarEstado(valorO(capacitacion.getEstado(), "PENDIENTE"));
        validarEstado(estado);
        Servicio servicio = acceso.servicioAutorizado(capacitacion.getServicio().getId());
        aplicar(capacitacion, capacitacion, servicio, estado, true);
        Capacitacion guardada = capacitacionRepository.save(capacitacion);
        logs.registrarLog("CREAR", "CAPACITACIONES", "Se creó la capacitación " + guardada.getId());
        return guardada;
    }

    @Transactional
    public Capacitacion actualizarCapacitacion(Long id, Capacitacion actualizada) {
        Capacitacion existente = capacitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capacitación no encontrada"));
        acceso.servicioAutorizado(existente.getServicio().getId());
        validarCapacitacion(actualizada);
        String estado = normalizarEstado(actualizada.getEstado());
        validarEstado(estado);
        Servicio servicio = acceso.servicioAutorizado(actualizada.getServicio().getId());
        aplicar(existente, actualizada, servicio, estado, false);
        Capacitacion guardada = capacitacionRepository.save(existente);
        logs.registrarLog("ACTUALIZAR", "CAPACITACIONES", "Se actualizó la capacitación " + id);
        return guardada;
    }

    @Transactional
    public void eliminarCapacitacion(Long id) {
        Capacitacion c = capacitacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Capacitación no encontrada"));
        acceso.servicioAutorizado(c.getServicio().getId());
        if ("COMPLETADA".equals(c.getEstado())) {
            throw new BadRequestException("Una capacitación completada debe conservarse como evidencia del SGSI");
        }
        if (!constanciaRepository.findByCapacitacionId(id).isEmpty()) {
            throw new BadRequestException("No se puede eliminar una capacitación con constancias emitidas");
        }
        List<ParticipanteCapacitacion> participantes = participanteRepository.findByCapacitacionIdOrderByNombreAsc(id);
        for (ParticipanteCapacitacion participante : participantes) {
            intentoRepository.deleteAll(intentoRepository.findByParticipanteIdOrderByFechaIntentoDesc(participante.getId()));
        }
        participanteRepository.deleteAll(participantes);
        preguntaRepository.deleteAll(preguntaRepository.findByCapacitacionIdOrderByOrdenAsc(id));
        moduloRepository.deleteAll(moduloRepository.findByCapacitacionIdOrderByOrdenAsc(id));
        capacitacionRepository.delete(c);
        logs.registrarLog("ELIMINAR", "CAPACITACIONES", "Se eliminó la capacitación en borrador " + id);
    }

    private void aplicar(Capacitacion destino, Capacitacion fuente, Servicio servicio, String estado, boolean nueva) {
        destino.setTitulo(fuente.getTitulo().trim());
        destino.setDescripcion(trim(fuente.getDescripcion()));
        destino.setMaterialUrl(trim(fuente.getMaterialUrl()));
        destino.setVideoUrl(trim(fuente.getVideoUrl()));
        destino.setObjetivo(trim(fuente.getObjetivo()));
        destino.setPublicoObjetivo(trim(fuente.getPublicoObjetivo()));
        destino.setPuntajeMinimo(fuente.getPuntajeMinimo() == null ? (nueva ? 80 : destino.getPuntajeMinimo())
                : Math.max(0, Math.min(100, fuente.getPuntajeMinimo())));
        destino.setCreadaPorRpm(nueva
                ? Boolean.TRUE.equals(fuente.getCreadaPorRpm())
                : Boolean.TRUE.equals(destino.getCreadaPorRpm()) || Boolean.TRUE.equals(fuente.getCreadaPorRpm()));
        if (nueva || fuente.getMotivoRpm() != null) destino.setMotivoRpm(trim(fuente.getMotivoRpm()));
        if (fuente.getFechaInicio() != null && fuente.getFechaLimite() != null
                && fuente.getFechaLimite().isBefore(fuente.getFechaInicio())) {
            throw new BadRequestException("La fecha límite no puede ser anterior a la fecha de inicio");
        }
        destino.setFechaInicio(fuente.getFechaInicio());
        destino.setFechaLimite(fuente.getFechaLimite());
        destino.setControlCodigo(trim(fuente.getControlCodigo()));
        destino.setRiesgoIdReferencia(fuente.getRiesgoIdReferencia());
        destino.setEstado(estado);
        destino.setServicio(servicio);
        if ("COMPLETADA".equals(estado)) {
            if (destino.getFechaFinalizacion() == null) destino.setFechaFinalizacion(LocalDateTime.now());
        } else {
            destino.setFechaFinalizacion(null);
        }
    }

    private void validarCapacitacion(Capacitacion c) {
        if (c == null) throw new BadRequestException("El cuerpo de la capacitación es obligatorio");
        if (c.getTitulo() == null || c.getTitulo().isBlank()) throw new BadRequestException("El título es obligatorio");
        if (c.getServicio() == null || c.getServicio().getId() == null) throw new BadRequestException("El servicio es obligatorio");
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new BadRequestException("Estado no válido. Use PENDIENTE, EN_PROCESO o COMPLETADA");
        }
    }

    private String normalizarEstado(String estado) {
        if (estado == null || estado.isBlank()) throw new BadRequestException("El estado es obligatorio");
        return estado.trim().toUpperCase();
    }
    private String valorO(String valor, String defecto) { return valor == null || valor.isBlank() ? defecto : valor; }
    private String trim(String valor) { return valor == null ? null : valor.trim(); }
}
