package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.models.ControlCatalogo;
import com.globalisosecurity.backend.services.CatalogoControlesService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/catalogo-controles")
public class CatalogoControlesController{
 private final CatalogoControlesService service; public CatalogoControlesController(CatalogoControlesService s){service=s;}
 @GetMapping public List<ControlCatalogo> listar(@RequestParam(required=false)String dominio){return service.listar(dominio);}
 @PostMapping("/cargar-base") @PreAuthorize("hasRole('ADMINISTRADOR')") public Map<String,Object> cargar(){return Map.of("creados",service.cargarCatalogoBase(),"total",service.listar(null).size());}
}
