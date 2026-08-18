package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.HallazgoRequest;
import com.globalisosecurity.backend.models.HallazgoAuditoria;
import com.globalisosecurity.backend.services.HallazgoAuditoriaService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/hallazgos")
public class HallazgoAuditoriaController{
 private final HallazgoAuditoriaService service; public HallazgoAuditoriaController(HallazgoAuditoriaService s){service=s;}
 @GetMapping("/servicio/{servicioId}") public List<HallazgoAuditoria> listar(@PathVariable Long servicioId){return service.listar(servicioId);}
 @PostMapping @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUDITOR')") public HallazgoAuditoria crear(@RequestBody HallazgoRequest r){return service.crear(r);}
 @PutMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUDITOR')") public HallazgoAuditoria actualizar(@PathVariable Long id,@RequestBody HallazgoRequest r){return service.actualizar(id,r);}
}
