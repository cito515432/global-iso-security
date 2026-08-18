package com.globalisosecurity.backend.controllers;

import com.globalisosecurity.backend.models.ConstanciaCapacitacion;
import com.globalisosecurity.backend.models.ParticipanteCapacitacion;
import com.globalisosecurity.backend.services.ConstanciaCapacitacionService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/constancias-capacitacion")
public class ConstanciaCapacitacionController {
    private final ConstanciaCapacitacionService service;
    public ConstanciaCapacitacionController(ConstanciaCapacitacionService service) { this.service = service; }

    @GetMapping public List<ConstanciaCapacitacion> todas() { return service.obtenerTodas(); }
    @GetMapping("/{id}") public ConstanciaCapacitacion porId(@PathVariable Long id) {
        return service.obtenerPorId(id).orElseThrow(() -> new com.globalisosecurity.backend.exceptions.ResourceNotFoundException("Constancia no encontrada"));
    }
    @GetMapping("/servicio/{servicioId}") public List<ConstanciaCapacitacion> porServicio(@PathVariable Long servicioId) { return service.obtenerPorServicio(servicioId); }
    @GetMapping("/capacitacion/{capacitacionId}") public List<ConstanciaCapacitacion> porCapacitacion(@PathVariable Long capacitacionId) { return service.obtenerPorCapacitacion(capacitacionId); }
    @GetMapping("/documento/{documento}") public List<ConstanciaCapacitacion> porDocumento(@PathVariable String documento) { return service.obtenerPorDocumento(documento); }
    @GetMapping("/verificar/{codigo}") public Map<String, Object> verificar(@PathVariable String codigo) { return service.verificarPublica(codigo); }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ConstanciaCapacitacion crear(@RequestBody ConstanciaCapacitacion c) { return service.crearConstancia(c); }

    @PostMapping("/participante/{participanteId}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ConstanciaCapacitacion emitir(@PathVariable Long participanteId) {
        ParticipanteCapacitacion p = new ParticipanteCapacitacion(); p.setId(participanteId);
        return service.emitirParaParticipante(p);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public ConstanciaCapacitacion actualizar(@PathVariable Long id,@RequestBody ConstanciaCapacitacion c) { return service.actualizarConstancia(id,c); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','CAPACITADOR')")
    public void eliminar(@PathVariable Long id) { service.eliminarConstancia(id); }

    @GetMapping(value="/{id}/pdf", produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        byte[] bytes = service.generarPdf(id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("constancia-global-iso-" + id + ".pdf", StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).body(bytes);
    }
}
