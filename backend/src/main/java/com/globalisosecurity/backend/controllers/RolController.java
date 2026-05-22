package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.dto.RolRequest;
import com.globalisosecurity.backend.models.Rol;
import com.globalisosecurity.backend.services.RolService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    @Autowired
    private RolService rolService;

    @GetMapping
    public ResponseEntity<List<Rol>> obtenerTodos() {
        return ResponseEntity.ok(rolService.obtenerTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Rol> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(rolService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Rol> crear(@RequestBody RolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crearRol(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Rol> actualizar(@PathVariable Long id, @RequestBody RolRequest request) {
        return ResponseEntity.ok(rolService.actualizarRol(id, request));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Rol> cambiarEstado(
            @PathVariable Long id,
            @RequestParam(required = false) Boolean activo,
            @RequestBody(required = false) Map<String, Boolean> body) {

        Boolean nuevoEstado = activo;
        if (nuevoEstado == null && body != null) {
            nuevoEstado = body.get("activo");
        }

        return ResponseEntity.ok(rolService.cambiarEstado(id, nuevoEstado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        rolService.eliminarRol(id);
        return ResponseEntity.ok("Rol eliminado correctamente");
    }
}
