package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.services.EmpresaService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {
    private final EmpresaService service;
    public EmpresaController(EmpresaService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public List<Empresa> obtenerTodas() { return service.obtenerTodas(); }

    @GetMapping("/asignadas")
    public List<Empresa> obtenerAsignadas() { return service.obtenerEmpresasAsignadas(); }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Long id) {
        return service.obtenerPorId(id).<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body("Empresa no encontrada"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Empresa crear(@RequestBody Empresa empresa) { return service.crearEmpresa(empresa); }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public Empresa actualizar(@PathVariable Long id, @RequestBody Empresa empresa) { return service.actualizarEmpresa(id, empresa); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public void eliminar(@PathVariable Long id) { service.eliminarEmpresa(id); }
}
