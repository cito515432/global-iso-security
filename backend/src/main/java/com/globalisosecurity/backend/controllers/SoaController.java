package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.SoaControlUpdateRequest;
import com.globalisosecurity.backend.services.SoaService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/soa")
public class SoaController{
 private final SoaService service; public SoaController(SoaService s){service=s;}
 @PostMapping("/servicio/{servicioId}/inicializar") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Map<String,Object> inicializar(@PathVariable Long servicioId){return service.inicializar(servicioId);}
 @GetMapping("/servicio/{servicioId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR','CAPACITADOR','USUARIO_EMPRESA','USUARIO')") public List<Map<String,Object>> listar(@PathVariable Long servicioId){return service.listar(servicioId);}
 @GetMapping("/servicio/{servicioId}/resumen") public Map<String,Object> resumen(@PathVariable Long servicioId){return service.resumen(servicioId);}
 @GetMapping("/{id}") public Map<String,Object> obtener(@PathVariable Long id){return service.obtener(id);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Map<String,Object> actualizar(@PathVariable Long id,@RequestBody SoaControlUpdateRequest r){return service.actualizar(id,r);}
}
