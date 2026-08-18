package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.dto.ServicioResponseDTO;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.services.ServicioService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {
    private final ServicioService service;
    public ServicioController(ServicioService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<ServicioResponseDTO> obtenerTodos(@RequestParam(required=false) Long empresaId) { return service.listarServiciosDTO(empresaId); }

    @GetMapping("/mi-servicio") public Map<String,Object> miServicio() { return service.obtenerMiServicio(); }
    @GetMapping("/{servicioId}/resumen") public Map<String,Object> resumen(@PathVariable Long servicioId) { return service.obtenerResumen(servicioId); }
    @GetMapping("/{servicioId}/estado-completo") public Map<String,Object> estado(@PathVariable Long servicioId) { return service.obtenerEstadoCompleto(servicioId); }

    @GetMapping("/{id}") public ResponseEntity<?> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Servicio no encontrado"));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Servicio> porEstado(@PathVariable String estado) { return service.obtenerPorEstado(estado); }

    @GetMapping("/empresa/{empresaId}") public List<Servicio> porEmpresa(@PathVariable Long empresaId) { return service.obtenerPorEmpresa(empresaId); }

    @PostMapping @PreAuthorize("hasRole('ADMINISTRADOR')") public Servicio crear(@RequestBody Servicio servicio) { return service.crearServicio(servicio); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Servicio actualizar(@PathVariable Long id,@RequestBody Servicio servicio) { return service.actualizarServicio(id,servicio); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public void eliminar(@PathVariable Long id) { service.eliminarServicio(id); }
}
