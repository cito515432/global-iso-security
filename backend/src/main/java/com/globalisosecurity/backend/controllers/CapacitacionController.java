package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.models.Capacitacion;
import com.globalisosecurity.backend.services.CapacitacionService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/capacitaciones")
public class CapacitacionController {
    private final CapacitacionService service;
    public CapacitacionController(CapacitacionService service) { this.service = service; }

    @GetMapping public List<Capacitacion> obtenerTodas() { return service.obtenerTodas(); }
    @GetMapping("/{id}") public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Capacitación no encontrada"));
    }
    @GetMapping("/estado/{estado}") public List<Capacitacion> obtenerPorEstado(@PathVariable String estado) { return service.obtenerPorEstado(estado); }
    @GetMapping("/servicio/{servicioId}") public List<Capacitacion> obtenerPorServicio(@PathVariable Long servicioId) { return service.obtenerPorServicio(servicioId); }
    @GetMapping("/empresa/{empresaId}") public List<Capacitacion> obtenerPorEmpresa(@PathVariable Long empresaId) { return service.obtenerPorEmpresa(empresaId); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public Capacitacion crear(@RequestBody Capacitacion capacitacion) { return service.crearCapacitacion(capacitacion); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public Capacitacion actualizar(@PathVariable Long id, @RequestBody Capacitacion capacitacion) { return service.actualizarCapacitacion(id, capacitacion); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public void eliminar(@PathVariable Long id) { service.eliminarCapacitacion(id); }
}
