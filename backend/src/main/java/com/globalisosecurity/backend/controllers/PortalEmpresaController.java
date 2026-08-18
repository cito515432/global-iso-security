package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.PortalEmpresaDTO;
import com.globalisosecurity.backend.services.PortalEmpresaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/portal-empresa")
public class PortalEmpresaController{
 private final PortalEmpresaService service; public PortalEmpresaController(PortalEmpresaService s){service=s;}
 @GetMapping("/mi-resumen") @PreAuthorize("hasAnyRole('USUARIO_EMPRESA','USUARIO')") public PortalEmpresaDTO miResumen(){return service.miResumen();}
 @GetMapping("/empresa/{empresaId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR','CAPACITADOR','USUARIO_EMPRESA','USUARIO')") public PortalEmpresaDTO resumen(@PathVariable Long empresaId){return service.resumen(empresaId);}
}
