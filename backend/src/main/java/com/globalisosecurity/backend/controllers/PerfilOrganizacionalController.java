package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.PerfilOrganizacionalRequest;
import com.globalisosecurity.backend.models.PerfilOrganizacional;
import com.globalisosecurity.backend.services.PerfilOrganizacionalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/contexto")
public class PerfilOrganizacionalController{
 private final PerfilOrganizacionalService service; public PerfilOrganizacionalController(PerfilOrganizacionalService s){service=s;}
 @GetMapping("/empresa/{empresaId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','AUDITOR','CAPACITADOR','USUARIO_EMPRESA','USUARIO')") public PerfilOrganizacional obtener(@PathVariable Long empresaId){return service.obtener(empresaId);}
 @PutMapping("/empresa/{empresaId}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR','USUARIO_EMPRESA','USUARIO')") public PerfilOrganizacional guardar(@PathVariable Long empresaId,@RequestBody PerfilOrganizacionalRequest r){return service.guardar(empresaId,r);}
}
