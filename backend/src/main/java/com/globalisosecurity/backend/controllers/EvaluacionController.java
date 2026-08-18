package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.models.Evaluacion;
import com.globalisosecurity.backend.services.EvaluacionService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {
    private final EvaluacionService service;
    public EvaluacionController(EvaluacionService service){this.service=service;}

    @GetMapping @PreAuthorize("hasRole('ADMINISTRADOR')") public List<Evaluacion> todas(){return service.obtenerTodas();}
    @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public ResponseEntity<?> una(@PathVariable Long id){Optional<Evaluacion> x=service.obtenerPorId(id);return x.<ResponseEntity<?>>map(ResponseEntity::ok).orElseGet(()->ResponseEntity.notFound().build());}
    @GetMapping("/estado/{estado}") @PreAuthorize("hasRole('ADMINISTRADOR')") public List<Evaluacion> estado(@PathVariable String estado){return service.obtenerPorEstado(estado);}
    @GetMapping("/servicio/{servicioId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public List<Evaluacion> servicio(@PathVariable Long servicioId){return service.obtenerPorServicio(servicioId);}
    @GetMapping("/empresa/{empresaId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public List<Evaluacion> empresa(@PathVariable Long empresaId){return service.obtenerPorEmpresa(empresaId);}
    @PostMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Evaluacion crear(@RequestBody Evaluacion e){return service.crearEvaluacion(e);}
    @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public Evaluacion actualizar(@PathVariable Long id,@RequestBody Evaluacion e){return service.actualizarEvaluacion(id,e);}
    @DeleteMapping("/{id}") @PreAuthorize("hasRole('ADMINISTRADOR')") public void eliminar(@PathVariable Long id){service.eliminarEvaluacion(id);}
}
