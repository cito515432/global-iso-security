package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.HallazgoRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import com.globalisosecurity.backend.utils.SecurityUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HallazgoAuditoriaService {
    private static final Set<String> SEVERIDADES=Set.of("BAJA","MEDIA","ALTA","CRITICA");
    private static final Set<String> ESTADOS=Set.of("ABIERTO","EN_TRATAMIENTO","CERRADO");
    private final HallazgoAuditoriaRepository repository; private final SoaControlRepository soaRepository; private final RiesgoRepository riesgoRepository; private final AccesoEmpresaService acceso; private final LogAuditoriaService logs;
    public HallazgoAuditoriaService(HallazgoAuditoriaRepository repository,SoaControlRepository soaRepository,RiesgoRepository riesgoRepository,AccesoEmpresaService acceso,LogAuditoriaService logs){this.repository=repository;this.soaRepository=soaRepository;this.riesgoRepository=riesgoRepository;this.acceso=acceso;this.logs=logs;}
    public List<HallazgoAuditoria> listar(Long servicioId){acceso.servicioAutorizado(servicioId);return repository.findByServicioIdOrderByFechaDeteccionDesc(servicioId);}
    @Transactional public HallazgoAuditoria crear(HallazgoRequest q){if(q==null||q.servicioId()==null||q.titulo()==null||q.titulo().isBlank()||q.descripcion()==null||q.descripcion().isBlank())throw new BadRequestException("Servicio, título y descripción son obligatorios");Servicio s=acceso.servicioAutorizado(q.servicioId());HallazgoAuditoria h=new HallazgoAuditoria();h.setServicio(s);aplicar(h,q);h.setCreadoPor(SecurityUtils.getUsuarioActual());repository.save(h);logs.registrarLog("CREAR","AUDITORIA","Se registró hallazgo "+h.getTitulo());return h;}
    @Transactional public HallazgoAuditoria actualizar(Long id,HallazgoRequest q){HallazgoAuditoria h=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Hallazgo no encontrado"));acceso.servicioAutorizado(h.getServicio().getId());aplicar(h,q);if("CERRADO".equals(h.getEstado())&&h.getFechaCierre()==null)h.setFechaCierre(LocalDateTime.now());repository.save(h);return h;}
    private void aplicar(HallazgoAuditoria h,HallazgoRequest q){if(q.soaControlId()!=null){SoaControl so=soaRepository.findById(q.soaControlId()).orElseThrow(()->new ResourceNotFoundException("Control SoA no encontrado"));if(!so.getServicio().getId().equals(h.getServicio().getId()))throw new BadRequestException("El control no pertenece al servicio");h.setSoaControl(so);}if(q.riesgoId()!=null){Riesgo r=riesgoRepository.findById(q.riesgoId()).orElseThrow(()->new ResourceNotFoundException("Riesgo no encontrado"));if(!r.getServicio().getId().equals(h.getServicio().getId()))throw new BadRequestException("El riesgo no pertenece al servicio");h.setRiesgo(r);}if(q.titulo()!=null)h.setTitulo(q.titulo().trim());if(q.descripcion()!=null)h.setDescripcion(q.descripcion().trim());String sev=norm(q.severidad(),h.getSeveridad());String est=norm(q.estado(),h.getEstado());if(!SEVERIDADES.contains(sev))throw new BadRequestException("Severidad no válida");if(!ESTADOS.contains(est))throw new BadRequestException("Estado no válido");h.setSeveridad(sev);h.setEstado(est);h.setRecurrente(Boolean.TRUE.equals(q.recurrente()));}
    private String norm(String s,String f){return s==null||s.isBlank()?f:s.trim().toUpperCase();}
}
