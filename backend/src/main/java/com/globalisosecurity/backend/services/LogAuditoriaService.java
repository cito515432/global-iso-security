/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.LogAuditoria;
import com.globalisosecurity.backend.repositories.LogAuditoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LogAuditoriaService {

    @Autowired
    private LogAuditoriaRepository logAuditoriaRepository;

    public List<LogAuditoria> obtenerTodos() {
        return logAuditoriaRepository.findAll();
    }

    public Optional<LogAuditoria> obtenerPorId(Long id) {
        return logAuditoriaRepository.findById(id);
    }

    public List<LogAuditoria> obtenerPorModulo(String modulo) {
        if (modulo == null || modulo.trim().isEmpty()) {
            throw new BadRequestException("El módulo es obligatorio");
        }
        return logAuditoriaRepository.findByModulo(modulo.trim());
    }

    public List<LogAuditoria> obtenerPorUsuario(String usuario) {
        if (usuario == null || usuario.trim().isEmpty()) {
            throw new BadRequestException("El usuario es obligatorio");
        }
        return logAuditoriaRepository.findByUsuario(usuario.trim());
    }

    public List<LogAuditoria> obtenerPorAccion(String accion) {
        if (accion == null || accion.trim().isEmpty()) {
            throw new BadRequestException("La acción es obligatoria");
        }
        return logAuditoriaRepository.findByAccion(accion.trim());
    }

    public LogAuditoria crearLog(LogAuditoria log) {
        validarLog(log);

        if (log.getFecha() == null) {
            log.setFecha(LocalDateTime.now());
        }

        log.setUsuario(log.getUsuario().trim());
        log.setAccion(log.getAccion().trim());
        log.setModulo(log.getModulo().trim());
        log.setDescripcion(log.getDescripcion().trim());
        log.setIp(log.getIp().trim());

        return logAuditoriaRepository.save(log);
    }

    public void eliminarLog(Long id) {
        if (!logAuditoriaRepository.existsById(id)) {
            throw new ResourceNotFoundException("Log de auditoría no encontrado");
        }

        logAuditoriaRepository.deleteById(id);
    }

    private void validarLog(LogAuditoria log) {
        if (log.getUsuario() == null || log.getUsuario().trim().isEmpty()) {
            throw new BadRequestException("El usuario es obligatorio");
        }

        if (log.getAccion() == null || log.getAccion().trim().isEmpty()) {
            throw new BadRequestException("La acción es obligatoria");
        }

        if (log.getModulo() == null || log.getModulo().trim().isEmpty()) {
            throw new BadRequestException("El módulo es obligatorio");
        }

        if (log.getDescripcion() == null || log.getDescripcion().trim().isEmpty()) {
            throw new BadRequestException("La descripción es obligatoria");
        }

        if (log.getIp() == null || log.getIp().trim().isEmpty()) {
            throw new BadRequestException("La IP es obligatoria");
        }
    }
}
