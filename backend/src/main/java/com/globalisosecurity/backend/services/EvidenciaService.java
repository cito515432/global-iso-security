package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.EvidenciaValidacionRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.Evidencia;
import com.globalisosecurity.backend.models.SoaControl;
import com.globalisosecurity.backend.repositories.EvidenciaRepository;
import com.globalisosecurity.backend.repositories.SoaControlRepository;
import com.globalisosecurity.backend.utils.SecurityUtils;
import java.io.InputStream;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class EvidenciaService {
    private static final Set<String> ESTADOS=Set.of("PENDIENTE","VALIDADA","RECHAZADA");
    private final EvidenciaRepository repository;
    private final SoaControlRepository soaRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;
    private final Path storage;

    public record Descarga(Evidencia evidencia, Resource recurso){}

    public EvidenciaService(EvidenciaRepository repository, SoaControlRepository soaRepository,
            AccesoEmpresaService acceso, LogAuditoriaService logs,
            @Value("${app.storage.evidencias:./storage/evidencias}") String storageDir) {
        this.repository=repository; this.soaRepository=soaRepository; this.acceso=acceso; this.logs=logs;
        this.storage=Paths.get(storageDir).toAbsolutePath().normalize();
        try { Files.createDirectories(this.storage); } catch(Exception e){ throw new IllegalStateException("No se pudo crear el directorio de evidencias",e); }
    }

    public List<Evidencia> listarPorServicio(Long servicioId){acceso.servicioAutorizado(servicioId);return repository.findByServicioIdOrderByFechaCargaDesc(servicioId);}
    public List<Evidencia> listarPorControl(Long soaId){SoaControl s=soaRepository.findById(soaId).orElseThrow(()->new ResourceNotFoundException("Control SoA no encontrado"));acceso.servicioAutorizado(s.getServicio().getId());return repository.findBySoaControlIdOrderByFechaCargaDesc(soaId);}

    @Transactional
    public Evidencia cargar(Long soaId, MultipartFile archivo, String descripcion, String tipo, LocalDate vencimiento) {
        if(archivo==null||archivo.isEmpty()) throw new BadRequestException("Debe seleccionar un archivo");
        if(archivo.getSize()>25L*1024*1024) throw new BadRequestException("El archivo supera el límite de 25 MB");
        SoaControl s=soaRepository.findById(soaId).orElseThrow(()->new ResourceNotFoundException("Control SoA no encontrado"));
        acceso.servicioAutorizado(s.getServicio().getId());
        String original=archivo.getOriginalFilename()==null?"evidencia":Paths.get(archivo.getOriginalFilename()).getFileName().toString();
        String ext="";int pos=original.lastIndexOf('.');if(pos>=0&&pos<original.length()-1)ext=original.substring(pos).replaceAll("[^A-Za-z0-9.]","");
        String stored=UUID.randomUUID()+ext;Path target=storage.resolve(stored).normalize();
        if(!target.startsWith(storage))throw new BadRequestException("Nombre de archivo no válido");
        try(InputStream in=archivo.getInputStream()){
            byte[] bytes=in.readAllBytes();Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            Evidencia e=new Evidencia();e.setServicio(s.getServicio());e.setSoaControl(s);e.setNombreOriginal(original);e.setNombreAlmacenado(stored);
            e.setRutaArchivo(target.toString());e.setTipoMime(archivo.getContentType());e.setHashSha256(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
            e.setDescripcion(trim(descripcion));e.setTipoEvidencia(tipo==null||tipo.isBlank()?"DOCUMENTO":tipo.trim().toUpperCase());e.setFechaVencimiento(vencimiento);
            e.setCargadaPor(SecurityUtils.getUsuarioActual());e.setEstado("PENDIENTE");repository.save(e);
            logs.registrarLog("CARGAR","EVIDENCIAS","Se cargó evidencia para "+s.getControl().getCodigo());return e;
        }catch(Exception ex){try{Files.deleteIfExists(target);}catch(Exception ignored){}throw new BadRequestException("No fue posible almacenar la evidencia: "+ex.getMessage());}
    }

    @Transactional
    public Evidencia validar(Long id,EvidenciaValidacionRequest req){
        Evidencia e=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Evidencia no encontrada"));acceso.servicioAutorizado(e.getServicio().getId());
        if(req==null||req.estado()==null)throw new BadRequestException("El estado es obligatorio");String estado=req.estado().trim().toUpperCase();
        if(!ESTADOS.contains(estado)||"PENDIENTE".equals(estado))throw new BadRequestException("Use VALIDADA o RECHAZADA");
        if("RECHAZADA".equals(estado)&&(req.observacion()==null||req.observacion().isBlank()))throw new BadRequestException("La observación es obligatoria al rechazar");
        e.setEstado(estado);e.setValidadaPor(SecurityUtils.getUsuarioActual());e.setFechaValidacion(LocalDateTime.now());e.setObservacionValidacion(trim(req.observacion()));repository.save(e);
        logs.registrarLog("VALIDAR","EVIDENCIAS","Evidencia "+id+" marcada como "+estado);return e;
    }

    public Descarga descargar(Long id){
        Evidencia e=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Evidencia no encontrada"));
        acceso.servicioAutorizado(e.getServicio().getId());
        Path p=Paths.get(e.getRutaArchivo()).toAbsolutePath().normalize();
        if(!p.startsWith(storage))throw new BadRequestException("La ruta de la evidencia no es válida");
        if(!Files.exists(p)||!Files.isRegularFile(p))throw new ResourceNotFoundException("El archivo físico no está disponible");
        return new Descarga(e,new FileSystemResource(p));
    }

    @Transactional
    public void eliminar(Long id){
        Evidencia e=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Evidencia no encontrada"));
        acceso.servicioAutorizado(e.getServicio().getId());
        repository.delete(e);
        try{
            Path p=Paths.get(e.getRutaArchivo()).toAbsolutePath().normalize();
            if(p.startsWith(storage))Files.deleteIfExists(p);
        }catch(Exception ignored){}
    }
    private String trim(String s){return s==null?null:s.trim();}
}
