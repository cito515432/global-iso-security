package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.dto.RiesgoControlRequest;
import com.globalisosecurity.backend.dto.RiesgoRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiesgoService {
    private static final Set<String> TRATAMIENTOS = Set.of("MITIGAR", "ACEPTAR", "TRANSFERIR", "EVITAR");
    private static final Set<String> ESTADOS = Set.of("ABIERTO", "EN_TRATAMIENTO", "ACEPTADO", "CERRADO");
    private final RiesgoRepository repository;
    private final RiesgoControlRepository relacionRepository;
    private final ControlCatalogoRepository controlRepository;
    private final AccesoEmpresaService acceso;
    private final LogAuditoriaService logs;

    public RiesgoService(RiesgoRepository repository, RiesgoControlRepository relacionRepository,
            ControlCatalogoRepository controlRepository, AccesoEmpresaService acceso, LogAuditoriaService logs) {
        this.repository=repository; this.relacionRepository=relacionRepository; this.controlRepository=controlRepository;
        this.acceso=acceso; this.logs=logs;
    }

    public List<Map<String,Object>> listar(Long servicioId) {
        acceso.servicioAutorizado(servicioId);
        return repository.findByServicioIdOrderByNivelInherenteDesc(servicioId).stream().map(this::mapear).toList();
    }

    public Map<String,Object> obtener(Long id) {
        Riesgo r=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Riesgo no encontrado"));
        acceso.servicioAutorizado(r.getServicio().getId()); return mapear(r);
    }

    @Transactional
    public Map<String,Object> crear(RiesgoRequest req) {
        validar(req);
        Servicio servicio=acceso.servicioAutorizado(req.servicioId());
        String codigo=req.codigo().trim().toUpperCase();
        if(repository.findByServicioIdAndCodigo(servicio.getId(),codigo).isPresent()) throw new BadRequestException("Ya existe un riesgo con ese código");
        Riesgo r=new Riesgo(); r.setServicio(servicio); aplicar(r,req,codigo); repository.save(r);
        logs.registrarLog("CREAR","RIESGOS","Se creó el riesgo "+codigo+" en el servicio "+servicio.getId());
        return mapear(r);
    }

    @Transactional
    public Map<String,Object> actualizar(Long id,RiesgoRequest req) {
        Riesgo r=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Riesgo no encontrado"));
        acceso.servicioAutorizado(r.getServicio().getId());
        if(req==null) throw new BadRequestException("El cuerpo es obligatorio");
        String codigo=req.codigo()==null||req.codigo().isBlank()?r.getCodigo():req.codigo().trim().toUpperCase();
        Optional<Riesgo> duplicado=repository.findByServicioIdAndCodigo(r.getServicio().getId(),codigo);
        if(duplicado.isPresent()&&!duplicado.get().getId().equals(id)) throw new BadRequestException("Ya existe un riesgo con ese código");
        aplicar(r,req,codigo); repository.save(r);
        logs.registrarLog("ACTUALIZAR","RIESGOS","Se actualizó el riesgo "+codigo);
        return mapear(r);
    }

    @Transactional
    public Map<String,Object> asociarControl(Long riesgoId,RiesgoControlRequest req) {
        Riesgo r=repository.findById(riesgoId).orElseThrow(()->new ResourceNotFoundException("Riesgo no encontrado"));
        acceso.servicioAutorizado(r.getServicio().getId());
        if(req==null||req.controlId()==null) throw new BadRequestException("El control es obligatorio");
        ControlCatalogo c=controlRepository.findById(req.controlId()).orElseThrow(()->new ResourceNotFoundException("Control no encontrado"));
        RiesgoControl rc=relacionRepository.findByRiesgoIdAndControlId(riesgoId,c.getId()).orElse(new RiesgoControl());
        rc.setRiesgo(r); rc.setControl(c); rc.setTipoRelacion(normalizar(req.tipoRelacion(),"TRATAMIENTO"));
        rc.setEficaciaEsperada(req.eficaciaEsperada()==null?50:Math.max(0,Math.min(100,req.eficaciaEsperada())));
        rc.setObservacion(trim(req.observacion())); relacionRepository.save(rc);
        logs.registrarLog("ASOCIAR", "RIESGOS", "Se relacionó el control " + c.getCodigo() + " con el riesgo " + r.getCodigo());
        return mapear(r);
    }

    @Transactional
    public void desasociarControl(Long riesgoId,Long controlId) {
        Riesgo r=repository.findById(riesgoId).orElseThrow(()->new ResourceNotFoundException("Riesgo no encontrado"));
        acceso.servicioAutorizado(r.getServicio().getId());
        RiesgoControl rc=relacionRepository.findByRiesgoIdAndControlId(riesgoId,controlId).orElseThrow(()->new ResourceNotFoundException("Relación no encontrada"));
        relacionRepository.delete(rc);
        logs.registrarLog("DESASOCIAR", "RIESGOS", "Se retiró el control " + controlId + " del riesgo " + riesgoId);
    }

    @Transactional
    public void eliminar(Long id) {
        Riesgo r=repository.findById(id).orElseThrow(()->new ResourceNotFoundException("Riesgo no encontrado"));
        acceso.servicioAutorizado(r.getServicio().getId()); relacionRepository.deleteByRiesgoId(id); repository.delete(r);
        logs.registrarLog("ELIMINAR", "RIESGOS", "Se eliminó el riesgo " + r.getCodigo());
    }

    private void validar(RiesgoRequest r){
        if(r==null||r.servicioId()==null) throw new BadRequestException("El servicio es obligatorio");
        if(r.codigo()==null||r.codigo().isBlank()) throw new BadRequestException("El código es obligatorio");
        if(r.nombre()==null||r.nombre().isBlank()) throw new BadRequestException("El nombre es obligatorio");
    }
    private void aplicar(Riesgo r,RiesgoRequest q,String codigo){
        r.setCodigo(codigo); if(q.nombre()!=null)r.setNombre(q.nombre().trim());
        r.setActivoInformacion(trim(q.activoInformacion())); r.setAmenaza(trim(q.amenaza())); r.setVulnerabilidad(trim(q.vulnerabilidad()));
        r.setConsecuencia(trim(q.consecuencia())); int p=clamp(q.probabilidad(),r.getProbabilidad()); int i=clamp(q.impacto(),r.getImpacto());
        r.setProbabilidad(p); r.setImpacto(i); r.setNivelInherente(p * i);
        int residual = q.nivelResidual() == null
                ? (r.getNivelResidual() == null ? p * i : r.getNivelResidual())
                : Math.max(1, Math.min(25, q.nivelResidual()));
        r.setNivelResidual(residual);
        String tratamiento = normalizar(q.tratamiento(), r.getTratamiento());
        String estado = normalizar(q.estado(), r.getEstado());
        if (!TRATAMIENTOS.contains(tratamiento)) throw new BadRequestException("Tratamiento no válido");
        if (!ESTADOS.contains(estado)) throw new BadRequestException("Estado de riesgo no válido");
        r.setTratamiento(tratamiento); r.setResponsable(trim(q.responsable())); r.setEstado(estado);
        r.setFechaRevision(q.fechaRevision());r.setDescripcion(trim(q.descripcion()));r.setActualizadoEn(LocalDateTime.now());
    }
    private Map<String,Object> mapear(Riesgo r){
        List<RiesgoControl> rel=relacionRepository.findByRiesgoId(r.getId()); Map<String,Object> m=new LinkedHashMap<>();
        m.put("id",r.getId());m.put("servicioId",r.getServicio().getId());m.put("codigo",r.getCodigo());m.put("nombre",r.getNombre());
        m.put("activoInformacion",safe(r.getActivoInformacion()));m.put("amenaza",safe(r.getAmenaza()));m.put("vulnerabilidad",safe(r.getVulnerabilidad()));m.put("consecuencia",safe(r.getConsecuencia()));
        m.put("probabilidad",r.getProbabilidad());m.put("impacto",r.getImpacto());m.put("nivelInherente",r.getNivelInherente());m.put("categoria",categoria(r.getNivelInherente()));
        m.put("nivelResidual",r.getNivelResidual());m.put("tratamiento",r.getTratamiento());m.put("responsable",safe(r.getResponsable()));m.put("estado",r.getEstado());m.put("fechaRevision",r.getFechaRevision());m.put("descripcion",safe(r.getDescripcion()));
        m.put("controles",rel.stream().map(x->Map.of("relacionId",x.getId(),"id",x.getControl().getId(),"codigo",x.getControl().getCodigo(),"titulo",x.getControl().getTitulo(),"tipoRelacion",x.getTipoRelacion(),"eficaciaEsperada",x.getEficaciaEsperada())).toList());return m;
    }
    public static String categoria(int n){return n>=20?"CRITICO":n>=12?"ALTO":n>=6?"MEDIO":"BAJO";}
    private int clamp(Integer v,Integer fallback){int x=v==null?(fallback==null?1:fallback):v;return Math.max(1,Math.min(5,x));}
    private String normalizar(String v,String f){return v==null||v.isBlank()?f:v.trim().toUpperCase();}
    private String trim(String v){return v==null?null:v.trim();} private String safe(String v){return v==null?"":v;}
}
