package com.globalisosecurity.backend.controllers;
import com.globalisosecurity.backend.dto.EvidenciaValidacionRequest;
import com.globalisosecurity.backend.models.Evidencia;
import com.globalisosecurity.backend.services.EvidenciaService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/evidencias")
public class EvidenciaController{
 private final EvidenciaService service; public EvidenciaController(EvidenciaService s){service=s;}
 @GetMapping("/servicio/{servicioId}") public List<Evidencia> listarServicio(@PathVariable Long servicioId){return service.listarPorServicio(servicioId);}
 @GetMapping("/soa/{soaId}") public List<Evidencia> listarControl(@PathVariable Long soaId){return service.listarPorControl(soaId);}
 @PostMapping(value="/soa/{soaId}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public Evidencia cargar(@PathVariable Long soaId,@RequestPart("archivo")MultipartFile archivo,@RequestParam(required=false)String descripcion,@RequestParam(required=false)String tipo,@RequestParam(required=false)LocalDate fechaVencimiento){return service.cargar(soaId,archivo,descripcion,tipo,fechaVencimiento);}
 @PutMapping("/{id}/validar") @PreAuthorize("hasAnyRole('ADMINISTRADOR','AUDITOR')") public Evidencia validar(@PathVariable Long id,@RequestBody EvidenciaValidacionRequest r){return service.validar(id,r);}
 @GetMapping("/{id}/descargar") public ResponseEntity<Resource> descargar(@PathVariable Long id){var d=service.descargar(id);MediaType mt=MediaType.APPLICATION_OCTET_STREAM;try{if(d.evidencia().getTipoMime()!=null)mt=MediaType.parseMediaType(d.evidencia().getTipoMime());}catch(Exception ignored){}ContentDisposition disposition=ContentDisposition.attachment().filename(d.evidencia().getNombreOriginal(),StandardCharsets.UTF_8).build();return ResponseEntity.ok().contentType(mt).header(HttpHeaders.CONTENT_DISPOSITION,disposition.toString()).body(d.recurso());}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('ADMINISTRADOR','IMPLEMENTADOR')") public void eliminar(@PathVariable Long id){service.eliminar(id);}
}
