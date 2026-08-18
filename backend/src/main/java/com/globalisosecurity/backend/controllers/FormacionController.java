package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.dto.*;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.services.FormacionService;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/formacion")
public class FormacionController {
    private final FormacionService service;
    public FormacionController(FormacionService service) { this.service = service; }

    @GetMapping("/servicio/{servicioId}/dashboard") public Map<String,Object> dashboard(@PathVariable Long servicioId) { return service.dashboard(servicioId); }
    @GetMapping("/servicio/{servicioId}") public List<Map<String,Object>> listar(@PathVariable Long servicioId) { return service.listar(servicioId); }
    @GetMapping("/capacitaciones/{id}") public Map<String,Object> detalle(@PathVariable Long id) { return service.detalle(id); }

    @PostMapping("/capacitaciones/{id}/modulos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ModuloCapacitacion crearModulo(@PathVariable Long id,@RequestBody ModuloCapacitacionRequest r) { return service.guardarModulo(id,null,r); }
    @PutMapping("/capacitaciones/{id}/modulos/{moduloId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ModuloCapacitacion editarModulo(@PathVariable Long id,@PathVariable Long moduloId,@RequestBody ModuloCapacitacionRequest r) { return service.guardarModulo(id,moduloId,r); }
    @DeleteMapping("/modulos/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public void eliminarModulo(@PathVariable Long id) { service.eliminarModulo(id); }

    @PostMapping("/capacitaciones/{id}/participantes")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ParticipanteCapacitacion crearParticipante(@PathVariable Long id,@RequestBody ParticipanteCapacitacionRequest r) { return service.guardarParticipante(id,null,r); }
    @PutMapping("/capacitaciones/{id}/participantes/{participanteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ParticipanteCapacitacion editarParticipante(@PathVariable Long id,@PathVariable Long participanteId,@RequestBody ParticipanteCapacitacionRequest r) { return service.guardarParticipante(id,participanteId,r); }
    @DeleteMapping("/participantes/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public void eliminarParticipante(@PathVariable Long id) { service.eliminarParticipante(id); }

    @GetMapping("/capacitaciones/{id}/preguntas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public List<PreguntaCapacitacion> preguntas(@PathVariable Long id) { return service.listarPreguntas(id); }
    @PostMapping("/capacitaciones/{id}/preguntas")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public PreguntaCapacitacion crearPregunta(@PathVariable Long id,@RequestBody PreguntaCapacitacionRequest r) { return service.guardarPregunta(id,null,r); }
    @PutMapping("/capacitaciones/{id}/preguntas/{preguntaId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public PreguntaCapacitacion editarPregunta(@PathVariable Long id,@PathVariable Long preguntaId,@RequestBody PreguntaCapacitacionRequest r) { return service.guardarPregunta(id,preguntaId,r); }
    @DeleteMapping("/preguntas/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public void eliminarPregunta(@PathVariable Long id) { service.eliminarPregunta(id); }

    @PostMapping("/capacitaciones/{id}/participantes/{participanteId}/evaluar")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public Map<String,Object> evaluar(@PathVariable Long id,@PathVariable Long participanteId,@RequestBody IntentoCapacitacionRequest r) { return service.evaluar(id,participanteId,r); }
    @GetMapping("/participantes/{id}/intentos")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR','AUDITOR')")
    public List<Map<String,Object>> intentos(@PathVariable Long id) { return service.intentos(id); }
}
