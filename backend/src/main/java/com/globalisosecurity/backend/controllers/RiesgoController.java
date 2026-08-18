package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.RiesgoControlRequest;
import com.globalisosecurity.backend.dto.RiesgoRequest;
import com.globalisosecurity.backend.services.RiesgoService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/riesgos")
public class RiesgoController{
 private final RiesgoService service; public RiesgoController(RiesgoService s){service=s;}
 @GetMapping("/servicio/{servicioId}") public List<Map<String,Object>> listar(@PathVariable Long servicioId){return service.listar(servicioId);}
 @GetMapping("/{id}") public Map<String,Object> obtener(@PathVariable Long id){return service.obtener(id);}
 @PostMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Map<String,Object> crear(@RequestBody RiesgoRequest r){return service.crear(r);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Map<String,Object> actualizar(@PathVariable Long id,@RequestBody RiesgoRequest r){return service.actualizar(id,r);}
 @PostMapping("/{id}/controles") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Map<String,Object> asociar(@PathVariable Long id,@RequestBody RiesgoControlRequest r){return service.asociarControl(id,r);}
 @DeleteMapping("/{id}/controles/{controlId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public void desasociar(@PathVariable Long id,@PathVariable Long controlId){service.desasociarControl(id,controlId);}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public void eliminar(@PathVariable Long id){service.eliminar(id);}
}
