/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.LogAuditoria;
import com.globalisosecurity.backend.repositories.LogAuditoriaRepository;
import com.globalisosecurity.backend.utils.RequestUtils;
import com.globalisosecurity.backend.utils.SecurityUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LogAuditoriaService {

    private final LogAuditoriaRepository logAuditoriaRepository;

    public LogAuditoriaService(LogAuditoriaRepository logAuditoriaRepository) {
        this.logAuditoriaRepository = logAuditoriaRepository;
    }

    public List<LogAuditoria> obtenerTodos() {
        return logAuditoriaRepository.findTop200ByOrderByFechaDesc();
    }

    public Optional<LogAuditoria> obtenerPorId(Long id) {
        return logAuditoriaRepository.findById(id);
    }

    public List<LogAuditoria> obtenerPorModulo(String modulo) {
        return logAuditoriaRepository.findByModulo(modulo);
    }

    public List<LogAuditoria> obtenerPorUsuario(String usuario) {
        return logAuditoriaRepository.findByUsuario(usuario);
    }

    public List<LogAuditoria> obtenerPorAccion(String accion) {
        return logAuditoriaRepository.findByAccion(accion);
    }

    public LogAuditoria crearLog(LogAuditoria log) {
        if (log.getAccion() == null || log.getAccion().trim().isEmpty()) {
            throw new BadRequestException("La acción es obligatoria");
        }

        if (log.getModulo() == null || log.getModulo().trim().isEmpty()) {
            throw new BadRequestException("El módulo es obligatorio");
        }

        if (log.getDescripcion() == null || log.getDescripcion().trim().isEmpty()) {
            throw new BadRequestException("La descripción es obligatoria");
        }

        if (log.getFecha() == null) {
            log.setFecha(LocalDateTime.now());
        }

        if (log.getUsuario() == null || log.getUsuario().trim().isEmpty()) {
            log.setUsuario(SecurityUtils.getUsuarioActual());
        }

        if (log.getIp() == null || log.getIp().trim().isEmpty()) {
            log.setIp(RequestUtils.getClientIp());
        }

        return logAuditoriaRepository.save(log);
    }

    public void eliminarLog(Long id) {
        LogAuditoria log = logAuditoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Log de auditoría no encontrado"));

        logAuditoriaRepository.delete(log);
    }

    public void registrarLog(String accion, String modulo, String descripcion) {
        registrarEvento(SecurityUtils.getUsuarioActual(), accion, modulo, descripcion);
    }

    /** Records security events whose actor is not present in the authenticated request context. */
    public void registrarEvento(String usuario, String accion, String modulo, String descripcion) {
        LogAuditoria log = new LogAuditoria();
        log.setUsuario(clean(usuario, 255, "SISTEMA"));
        log.setAccion(accion);
        log.setModulo(modulo);
        log.setDescripcion(clean(descripcion, 1000, "Evento de seguridad"));
        log.setFecha(LocalDateTime.now());
        // Security-event IPs must not trust client-supplied X-Forwarded-For values.
        log.setIp(RequestUtils.getConnectionAddress());

        logAuditoriaRepository.save(log);
    }

    private static String clean(String value, int limit, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim().replaceAll("[\\p{Cntrl}]", " ");
        return normalized.length() <= limit ? normalized : normalized.substring(0, limit);
    }
}
