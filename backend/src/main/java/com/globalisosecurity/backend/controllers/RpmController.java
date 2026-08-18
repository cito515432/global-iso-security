package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.*;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.services.RpmEngineService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/rpm")
public class RpmController{
 private final RpmEngineService service; public RpmController(RpmEngineService s){service=s;}
 @PostMapping("/analizar/servicio/{servicioId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public Map<String,Object> analizarServicio(@PathVariable Long servicioId){return service.analizarServicio(servicioId);}
 @PostMapping("/analizar/soa/{soaId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR')") public RpmAnalisisDTO analizarControl(@PathVariable Long soaId){return service.analizarControl(soaId);}
 @GetMapping("/servicio/{servicioId}") public List<RpmAnalisisDTO> listar(@PathVariable Long servicioId){return service.listar(servicioId);}
 @GetMapping("/{id}") public RpmAnalisisDTO obtener(@PathVariable Long id){return service.obtener(id);}
 @PutMapping("/decisiones/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR','USUARIO_EMPRESA','USUARIO','CAPACITADOR')") public RpmDecision validar(@PathVariable Long id,@RequestBody RpmDecisionRequest r){return service.validarDecision(id,r);}
 @PostMapping("/decisiones/{id}/crear-capacitacion") @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')") public Capacitacion crearCapacitacion(@PathVariable Long id){return service.crearCapacitacion(id);}
 @PostMapping("/{id}/memoria") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR','USUARIO_EMPRESA','USUARIO')") public RpmMemoria memoria(@PathVariable Long id,@RequestBody RpmMemoriaRequest r){return service.registrarMemoria(id,r);}
}
