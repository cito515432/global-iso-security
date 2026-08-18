package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.services.SectorService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sectores")
public class SectorController {
    private final SectorService service;
    public SectorController(SectorService service) { this.service = service; }

    @GetMapping public List<Sector> obtenerTodos() { return service.obtenerTodos(); }
    @GetMapping("/{id}") public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Sector no encontrado"));
    }
    @PostMapping @PreAuthorize("hasRole('ADMINISTRADOR')") public Sector crear(@RequestBody Sector sector) { return service.crearSector(sector); }
    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public Sector actualizar(@PathVariable Long id,@RequestBody Sector sector) { return service.actualizarSector(id,sector); }
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public void eliminar(@PathVariable Long id) { service.eliminarSector(id); }
}
