package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.models.LogAuditoria;
import com.globalisosecurity.backend.services.LogAuditoriaService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Los logs son append-only desde la API pública: solo el backend puede escribirlos.
 * El administrador puede consultarlos, pero no crearlos ni eliminarlos mediante HTTP.
 */
@RestController
@RequestMapping("/api/logs-auditoria")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class LogAuditoriaController {

    private final LogAuditoriaService logAuditoriaService;

    public LogAuditoriaController(LogAuditoriaService logAuditoriaService) {
        this.logAuditoriaService = logAuditoriaService;
    }

    @GetMapping
    public List<LogAuditoria> obtenerTodos() {
        return logAuditoriaService.obtenerTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        Optional<LogAuditoria> log = logAuditoriaService.obtenerPorId(id);
        return log.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Log de auditoría no encontrado"));
    }

    @GetMapping("/modulo/{modulo}")
    public List<LogAuditoria> obtenerPorModulo(@PathVariable String modulo) {
        return logAuditoriaService.obtenerPorModulo(modulo);
    }

    @GetMapping("/usuario/{usuario}")
    public List<LogAuditoria> obtenerPorUsuario(@PathVariable String usuario) {
        return logAuditoriaService.obtenerPorUsuario(usuario);
    }

    @GetMapping("/accion/{accion}")
    public List<LogAuditoria> obtenerPorAccion(@PathVariable String accion) {
        return logAuditoriaService.obtenerPorAccion(accion);
    }
}
