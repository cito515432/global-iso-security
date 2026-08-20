package com.globalisosecurity.backend.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalisosecurity.backend.dto.RpmAnalisisDTO;
import com.globalisosecurity.backend.dto.RpmDecisionRequest;
import com.globalisosecurity.backend.dto.RpmMemoriaRequest;
import com.globalisosecurity.backend.exceptions.BadRequestException;
import com.globalisosecurity.backend.exceptions.ResourceNotFoundException;
import com.globalisosecurity.backend.models.*;
import com.globalisosecurity.backend.repositories.*;
import com.globalisosecurity.backend.utils.SecurityUtils;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RpmEngineService {
    private static final String VERSION="RPM-DETERMINISTA-1.0";
    private final RpmAnalisisRepository analisisRepository;
    private final RpmSenalRepository senalRepository;
    private final RpmDecisionRepository decisionRepository;
    private final RpmMemoriaRepository memoriaRepository;
    private final SoaControlRepository soaRepository;
    private final EvidenciaRepository evidenciaRepository;
    private final RiesgoControlRepository riesgoControlRepository;
    private final HallazgoAuditoriaRepository hallazgoRepository;
    private final CapacitacionRepository capacitacionRepository;
    private final ParticipanteCapacitacionRepository participanteRepository;
    private final AccesoEmpresaService acceso;
    private final SoaService soaService;
    private final FormacionService formacionService;
    private final LogAuditoriaService logs;
    private final ObjectMapper objectMapper;

    public RpmEngineService(RpmAnalisisRepository analisisRepository,RpmSenalRepository senalRepository,
            RpmDecisionRepository decisionRepository,RpmMemoriaRepository memoriaRepository,SoaControlRepository soaRepository,
            EvidenciaRepository evidenciaRepository,RiesgoControlRepository riesgoControlRepository,
            HallazgoAuditoriaRepository hallazgoRepository,CapacitacionRepository capacitacionRepository,
            ParticipanteCapacitacionRepository participanteRepository,AccesoEmpresaService acceso,SoaService soaService,
            FormacionService formacionService,LogAuditoriaService logs,ObjectMapper objectMapper){
        this.analisisRepository=analisisRepository;this.senalRepository=senalRepository;this.decisionRepository=decisionRepository;
        this.memoriaRepository=memoriaRepository;this.soaRepository=soaRepository;this.evidenciaRepository=evidenciaRepository;
        this.riesgoControlRepository=riesgoControlRepository;this.hallazgoRepository=hallazgoRepository;
        this.capacitacionRepository=capacitacionRepository;this.participanteRepository=participanteRepository;
        this.acceso=acceso;this.soaService=soaService;this.formacionService=formacionService;this.logs=logs;this.objectMapper=objectMapper;
    }

    @Transactional
    public Map<String,Object> analizarServicio(Long servicioId){
        acceso.servicioAutorizado(servicioId);
        if(soaRepository.countByServicioId(servicioId)==0)soaService.inicializar(servicioId);
        int analizados=0,reutilizados=0;
        for(SoaControl s:soaRepository.findByServicioIdOrderByControlCodigoAsc(servicioId)){
            if("NO_APLICABLE".equals(s.getAplicabilidad()))continue;
            Resultado r=analizarControlInterno(s);if(r.reutilizado)reutilizados++;else analizados++;
        }
        logs.registrarLog("EJECUTAR","RPM","Análisis RPM del servicio "+servicioId+": "+analizados+" nuevos, "+reutilizados+" sin cambios");
        return Map.of("servicioId",servicioId,"analisisNuevos",analizados,"sinCambios",reutilizados,"total",ultimosPorControl(servicioId).size());
    }

    @Transactional
    public RpmAnalisisDTO analizarControl(Long soaControlId){
        SoaControl s=soaRepository.findById(soaControlId).orElseThrow(()->new ResourceNotFoundException("Control SoA no encontrado"));
        acceso.servicioAutorizado(s.getServicio().getId());return mapear(analizarControlInterno(s).analisis);
    }

    public List<RpmAnalisisDTO> listar(Long servicioId){
        acceso.servicioAutorizado(servicioId);
        return ultimosPorControl(servicioId).stream().map(this::mapear).toList();
    }
    public RpmAnalisisDTO obtener(Long id){RpmAnalisis a=analisisRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Análisis RPM no encontrado"));acceso.servicioAutorizado(a.getServicio().getId());return mapear(a);}

    @Transactional
    public RpmDecision validarDecision(Long decisionId,RpmDecisionRequest req){
        RpmDecision d=decisionRepository.findById(decisionId).orElseThrow(()->new ResourceNotFoundException("Decisión RPM no encontrada"));acceso.servicioAutorizado(d.getAnalisis().getServicio().getId());
        if(req==null||req.estado()==null||req.estado().isBlank())throw new BadRequestException("El estado de validación es obligatorio");
        String estado=req.estado().trim().toUpperCase();Set<String> validos=Set.of("APROBADA","MODIFICADA","RECHAZADA","EN_EJECUCION","COMPLETADA");if(!validos.contains(estado))throw new BadRequestException("Estado de decisión no válido");
        if("RECHAZADA".equals(estado)&&(req.justificacion()==null||req.justificacion().isBlank()))throw new BadRequestException("La justificación es obligatoria para rechazar");
        if(req.accion()!=null&&!req.accion().isBlank())d.setAccion(req.accion().trim());if(req.tipoAccion()!=null&&!req.tipoAccion().isBlank())d.setTipoAccion(req.tipoAccion().trim().toUpperCase());
        d.setEstado(estado);d.setJustificacion(trim(req.justificacion()));d.setFechaObjetivo(req.fechaObjetivo());d.setValidadaPor(SecurityUtils.getUsuarioActual());d.setFechaValidacion(LocalDateTime.now());decisionRepository.save(d);
        List<RpmDecision> todas=decisionRepository.findByAnalisisId(d.getAnalisis().getId());boolean pendientes=todas.stream().anyMatch(x->"PENDIENTE".equals(x.getEstado()));if(!pendientes){d.getAnalisis().setEstado("VALIDADO");analisisRepository.save(d.getAnalisis());}
        logs.registrarLog("VALIDAR","RPM","Decisión RPM "+decisionId+" marcada como "+estado);return d;
    }

    @Transactional
    public Capacitacion crearCapacitacion(Long decisionId){
        RpmDecision d=decisionRepository.findById(decisionId).orElseThrow(()->new ResourceNotFoundException("Decisión RPM no encontrada"));acceso.servicioAutorizado(d.getAnalisis().getServicio().getId());
        if(!"CAPACITACION".equals(d.getTipoAccion()))throw new BadRequestException("La decisión no corresponde a una capacitación");
        if(!Set.of("APROBADA","MODIFICADA","EN_EJECUCION").contains(d.getEstado()))throw new BadRequestException("La recomendación debe ser validada antes de crear la capacitación");
        RpmAnalisis a=d.getAnalisis();String code=a.getSoaControl()!=null?a.getSoaControl().getControl().getCodigo():null;Long rid=a.getRiesgo()!=null?a.getRiesgo().getId():null;
        Capacitacion c=formacionService.crearDesdeRpm(a.getServicio(),code,rid,a.getExplicacion(),d.getAccion());d.setEstado("EN_EJECUCION");decisionRepository.save(d);return c;
    }

    @Transactional
    public RpmMemoria registrarMemoria(Long analisisId,RpmMemoriaRequest req){
        RpmAnalisis a=analisisRepository.findById(analisisId).orElseThrow(()->new ResourceNotFoundException("Análisis RPM no encontrado"));acceso.servicioAutorizado(a.getServicio().getId());
        if(req==null||req.resultado()==null||req.resultado().isBlank())throw new BadRequestException("El resultado es obligatorio para crear memoria");
        List<RpmDecision> decisiones=decisionRepository.findByAnalisisId(a.getId());String accion=decisiones.stream().filter(x->!"RECHAZADA".equals(x.getEstado())).map(RpmDecision::getAccion).collect(Collectors.joining(" | "));
        RpmMemoria m=new RpmMemoria();m.setAnalisis(a);m.setHuella(huellaSituacion(a,senalRepository.findByAnalisisIdOrderByPesoDesc(a.getId())));m.setPrioridadInicial(a.getPrioridad());m.setPrioridadFinal(req.prioridadFinal()==null?a.getPrioridad():req.prioridadFinal().trim().toUpperCase());m.setAccion(accion);m.setResultado(req.resultado().trim());m.setEfectividadPorcentaje(req.efectividadPorcentaje()==null?null:Math.max(0,Math.min(100,req.efectividadPorcentaje())));
        try{Map<String,Object> situacion=new LinkedHashMap<>();situacion.put("servicioId",a.getServicio().getId());situacion.put("control",a.getSoaControl()!=null?a.getSoaControl().getControl().getCodigo():"");situacion.put("riesgo",a.getRiesgo()!=null?a.getRiesgo().getCodigo():"");situacion.put("senales",senalRepository.findByAnalisisIdOrderByPesoDesc(a.getId()).stream().map(RpmSenal::getCodigo).toList());situacion.put("prioridadRpm",a.getPrioridad());situacion.put("prioridadMl",a.getPrioridadMl());situacion.put("confianzaMl",a.getConfianzaMl());situacion.put("versionModeloMl",a.getVersionModeloMl());m.setSituacionJson(objectMapper.writeValueAsString(situacion));}catch(Exception e){m.setSituacionJson("{}");}
        memoriaRepository.save(m);a.setEstado("EVALUADO");analisisRepository.save(a);logs.registrarLog("MEMORIZAR","RPM","Se registró memoria inmunológica para análisis "+analisisId);return m;
    }

    private Resultado analizarControlInterno(SoaControl s){
        List<Evidencia> evidencias=evidenciaRepository.findBySoaControlIdOrderByFechaCargaDesc(s.getId());
        List<RiesgoControl> relaciones=riesgoControlRepository.findByControlIdAndRiesgoServicioId(s.getControl().getId(),s.getServicio().getId());
        List<HallazgoAuditoria> hallazgos=hallazgoRepository.findBySoaControlIdAndEstadoNot(s.getId(),"CERRADO");
        List<Capacitacion> caps=capacitacionRepository.findByServicioId(s.getServicio().getId()).stream().filter(c->s.getControl().getCodigo().equalsIgnoreCase(c.getControlCodigo()==null?"":c.getControlCodigo())).toList();
        List<SignalSpec> specs=new ArrayList<>();
        if("PENDIENTE".equals(s.getAplicabilidad()))add(specs,"ANTIGENO","APLICABILIDAD_PENDIENTE","La aplicabilidad del control aún no ha sido decidida.","SoA",8,s.getAplicabilidad());
        if("APLICABLE".equals(s.getAplicabilidad())||"PENDIENTE".equals(s.getAplicabilidad())){
            switch(s.getEstadoImplementacion()){
                case "NO_INICIADO"->add(specs,"ANTIGENO","CONTROL_NO_INICIADO","El control no presenta avance de implementación.","SoA",25,"0%");
                case "PLANIFICADO"->add(specs,"ANTIGENO","CONTROL_PLANIFICADO","El control está planificado pero aún no produce protección verificable.","SoA",14,s.getPorcentajeImplementacion()+"%");
                case "PARCIAL"->add(specs,"ANTIGENO","CONTROL_PARCIAL","El control está implementado parcialmente.","SoA",18,s.getPorcentajeImplementacion()+"%");
                case "NO_EFECTIVO"->add(specs,"PELIGRO","CONTROL_NO_EFECTIVO","El control existe pero fue considerado no efectivo.","SoA",30,s.getPorcentajeImplementacion()+"%");
                default->{}
            }
            if(s.getPorcentajeImplementacion()<50 && !"NO_INICIADO".equals(s.getEstadoImplementacion()))add(specs,"PELIGRO","AVANCE_INSUFICIENTE","El avance informado es inferior al 50%.","SoA",8,s.getPorcentajeImplementacion()+"%");
            if(evidencias.isEmpty()&&s.getPorcentajeImplementacion()>0)add(specs,"ANTIGENO","SIN_EVIDENCIA","Existe avance declarado sin evidencia registrada.","Evidencias",18,"0 archivos");
            long pendientes=evidencias.stream().filter(e->"PENDIENTE".equals(e.getEstado())).count();if(pendientes>0)add(specs,"ANTIGENO","EVIDENCIA_PENDIENTE","Hay evidencias todavía no revisadas por auditoría.","Evidencias",Math.min(12,5+(int)pendientes),String.valueOf(pendientes));
            long rechazadas=evidencias.stream().filter(e->"RECHAZADA".equals(e.getEstado())).count();if(rechazadas>0)add(specs,"PELIGRO","EVIDENCIA_RECHAZADA","Una o más evidencias fueron rechazadas.","Evidencias",Math.min(24,14+(int)rechazadas*3),String.valueOf(rechazadas));
            long vencidas=evidencias.stream().filter(e->e.getFechaVencimiento()!=null&&e.getFechaVencimiento().isBefore(LocalDate.now())).count();if(vencidas>0)add(specs,"PELIGRO","EVIDENCIA_VENCIDA","Existen evidencias con vigencia expirada.","Evidencias",12,String.valueOf(vencidas));
        }
        Riesgo principal=relaciones.stream().map(RiesgoControl::getRiesgo).max(Comparator.comparingInt(Riesgo::getNivelInherente)).orElse(null);
        if(principal!=null){String cat=RiesgoService.categoria(principal.getNivelInherente());if("CRITICO".equals(cat))add(specs,"PELIGRO","RIESGO_CRITICO","El control está relacionado con un riesgo inherente crítico.","Riesgos",30,principal.getCodigo());else if("ALTO".equals(cat))add(specs,"PELIGRO","RIESGO_ALTO","El control está relacionado con un riesgo inherente alto.","Riesgos",20,principal.getCodigo());else if("MEDIO".equals(cat))add(specs,"PELIGRO","RIESGO_MEDIO","El control está relacionado con un riesgo inherente medio.","Riesgos",8,principal.getCodigo());}
        if(!hallazgos.isEmpty()){int peso=hallazgos.stream().mapToInt(h->"CRITICA".equals(h.getSeveridad())?25:"ALTA".equals(h.getSeveridad())?18:8).max().orElse(8);add(specs,"ANTIGENO","HALLAZGO_ABIERTO","Existen hallazgos de auditoría abiertos asociados al control.","Auditoría",peso,String.valueOf(hallazgos.size()));}
        long recurrentes=hallazgos.stream().filter(h->Boolean.TRUE.equals(h.getRecurrente())).count();if(recurrentes>0)add(specs,"PELIGRO","HALLAZGO_RECURRENTE","La misma condición ha sido observada de forma recurrente.","Auditoría",15,String.valueOf(recurrentes));
        if(s.getFechaObjetivo()!=null&&s.getFechaObjetivo().isBefore(LocalDate.now())&&!"IMPLEMENTADO".equals(s.getEstadoImplementacion()))add(specs,"PELIGRO","FECHA_VENCIDA","La fecha objetivo del control ya venció.","Plan de implementación",15,s.getFechaObjetivo().toString());
        if(s.getPuntajeRelevancia()>=60)add(specs,"CONTEXTO","RELEVANCIA_CONTEXTUAL","El control tiene alta relevancia según sector y contexto organizacional.","Contexto",5,String.valueOf(s.getPuntajeRelevancia()));
        boolean humano=tags(s.getControl()).stream().anyMatch(t->Set.of("personas","capacitacion","identidad","acceso").contains(t));
        if(humano&&!hallazgos.isEmpty()){
            if(caps.isEmpty())add(specs,"PELIGRO","BRECHA_FORMACION","Hay una condición humana asociada y no existe capacitación vinculada al control.","Formación",12,"sin programa");
            else{List<ParticipanteCapacitacion> ps=caps.stream().flatMap(c->participanteRepository.findByCapacitacionIdOrderByNombreAsc(c.getId()).stream()).toList();double prog=ps.stream().mapToInt(ParticipanteCapacitacion::getProgresoPorcentaje).average().orElse(0);if(ps.isEmpty()||prog<80)add(specs,"PELIGRO","BRECHA_FORMACION","La cobertura o finalización de formación asociada es insuficiente.","Formación",12,Math.round(prog)+"%");}
        }
        int score=Math.min(100,specs.stream().mapToInt(SignalSpec::peso).sum());String prioridad=score>=75?"CRITICA":score>=50?"ALTA":score>=25?"MEDIA":"BAJA";
        String input=buildInput(s,evidencias,relaciones,hallazgos,caps,specs);String hash=sha256(input);Optional<RpmAnalisis> previa=analisisRepository.findFirstBySoaControlIdOrderByGeneradoEnDesc(s.getId());if(previa.isPresent()&&hash.equals(previa.get().getHuellaEntrada()))return new Resultado(previa.get(),true);
        RpmAnalisis a=new RpmAnalisis();a.setServicio(s.getServicio());a.setSoaControl(s);a.setRiesgo(principal);a.setPuntaje(score);a.setPrioridad(prioridad);a.setVersionMotor(VERSION);a.setHuellaEntrada(hash);a.setResumen(resumen(s,score,prioridad,specs));a.setExplicacion(explicacion(s,specs,principal));analisisRepository.save(a);
        for(SignalSpec sp:specs){RpmSenal sn=new RpmSenal();sn.setAnalisis(a);sn.setCategoria(sp.categoria);sn.setCodigo(sp.codigo);sn.setDescripcion(sp.descripcion);sn.setFuente(sp.fuente);sn.setPeso(sp.peso);sn.setValor(sp.valor);senalRepository.save(sn);}crearDecisiones(a,s,specs,humano);return new Resultado(a,false);
    }

    private void crearDecisiones(RpmAnalisis a,SoaControl s,List<SignalSpec> specs,boolean humano){Set<String> codes=specs.stream().map(SignalSpec::codigo).collect(Collectors.toSet());List<RpmDecision> ds=new ArrayList<>();
        if(codes.stream().anyMatch(x->x.contains("EVIDENCIA"))||codes.contains("SIN_EVIDENCIA"))ds.add(decision(a,"REVISAR_EVIDENCIA","Completar, actualizar o someter a validación las evidencias asociadas al control "+s.getControl().getCodigo()+"."));
        if(codes.stream().anyMatch(x->x.startsWith("CONTROL_")||x.equals("FECHA_VENCIDA")))ds.add(decision(a,"PLAN_TRATAMIENTO","Definir o actualizar un plan de implementación con responsable, actividades, recursos y fecha objetivo para "+s.getControl().getCodigo()+"."));
        if(codes.stream().anyMatch(x->x.startsWith("RIESGO_")))ds.add(decision(a,"TRATAR_RIESGO","Revisar el tratamiento de los riesgos relacionados y verificar que el control reduzca el nivel residual esperado."));
        if(humano&&(codes.contains("BRECHA_FORMACION")||codes.contains("HALLAZGO_RECURRENTE")))ds.add(decision(a,"CAPACITACION","Diseñar y asignar una capacitación focalizada, medir aprendizaje y comprobar posteriormente si disminuyen los hallazgos asociados."));
        if("CRITICA".equals(a.getPrioridad()))ds.add(decision(a,"ESCALAR","Escalar la condición al responsable del SGSI para decisión prioritaria y seguimiento ejecutivo."));
        if(ds.isEmpty())ds.add(decision(a,"MONITOREAR","Mantener seguimiento periódico y conservar evidencias actualizadas del control."));ds.forEach(decisionRepository::save);
    }
    private RpmDecision decision(RpmAnalisis a,String tipo,String accion){RpmDecision d=new RpmDecision();d.setAnalisis(a);d.setTipoAccion(tipo);d.setAccion(accion);return d;}
    private List<RpmAnalisis> ultimosPorControl(Long servicioId){
        Map<Long,RpmAnalisis> latest=new LinkedHashMap<>();
        for(RpmAnalisis a:analisisRepository.findByServicioIdOrderByGeneradoEnDesc(servicioId)){
            Long key=a.getSoaControl()!=null?a.getSoaControl().getId():-a.getId();
            latest.putIfAbsent(key,a);
        }
        return latest.values().stream()
                .sorted(Comparator.comparingInt(RpmAnalisis::getPuntaje).reversed()
                        .thenComparing(RpmAnalisis::getGeneradoEn,Comparator.reverseOrder()))
                .toList();
    }

    private RpmAnalisisDTO mapear(RpmAnalisis a){List<RpmSenal> ss=senalRepository.findByAnalisisIdOrderByPesoDesc(a.getId());List<RpmDecision> ds=decisionRepository.findByAnalisisId(a.getId());String fp=huellaSituacion(a,ss);List<RpmMemoria> mem=memoriaRepository.findTop10ByHuellaOrderByCreadoEnDesc(fp);
        return new RpmAnalisisDTO(a.getId(),a.getServicio().getId(),a.getSoaControl()!=null?a.getSoaControl().getId():null,a.getSoaControl()!=null?a.getSoaControl().getControl().getCodigo():null,a.getSoaControl()!=null?a.getSoaControl().getControl().getTitulo():null,a.getRiesgo()!=null?a.getRiesgo().getId():null,a.getRiesgo()!=null?a.getRiesgo().getCodigo():null,a.getGeneradoEn(),a.getPuntaje(),a.getPrioridad(),a.getEstado(),a.getResumen(),a.getExplicacion(),a.getVersionMotor(),mapearMl(a),ss.stream().map(s->new RpmAnalisisDTO.Senal(s.getId(),s.getCategoria(),s.getCodigo(),s.getDescripcion(),s.getFuente(),s.getPeso(),s.getValor())).toList(),ds.stream().map(d->new RpmAnalisisDTO.Decision(d.getId(),d.getTipoAccion(),d.getAccion(),d.getEstado(),d.getValidadaPor(),d.getFechaValidacion(),d.getJustificacion(),d.getFechaObjetivo()==null?null:d.getFechaObjetivo().toString())).toList(),mem.stream().map(m->new RpmAnalisisDTO.MemoriaSimilar(m.getId(),m.getPrioridadFinal(),m.getAccion(),m.getResultado(),m.getEfectividadPorcentaje(),m.getCreadoEn())).toList());}

    private RpmAnalisisDTO.MlPrediction mapearMl(RpmAnalisis a){
        Map<String,Double> probs=Map.of();
        if(a.getProbabilidadesMl()!=null&&!a.getProbabilidadesMl().isBlank()){try{probs=objectMapper.readValue(a.getProbabilidadesMl(),new TypeReference<Map<String,Double>>(){});}catch(Exception ignored){probs=Map.of();}}
        if(a.getPrioridadMl()==null||a.getPrioridadMl().isBlank()){return new RpmAnalisisDTO.MlPrediction(null,null,probs,a.getVersionModeloMl(),a.getMlEstado()==null?"PENDIENTE":a.getMlEstado(),a.getMlGeneradoEn(),null,true,"Estimación ML pendiente o no disponible. El RPM determinista continúa operativo.");}
        boolean coincide=a.getPrioridad().equals(a.getPrioridadMl());double conf=a.getConfianzaMl()==null?0:a.getConfianzaMl();boolean revisar=!coincide||conf<0.70;String rec=!coincide?"RPM y ML difieren; la revisión humana tiene prioridad antes de ejecutar una respuesta.":conf<0.70?"RPM y ML coinciden, pero la confianza estimada del ML es baja; validar con cautela.":"RPM y ML coinciden. Mantener la validación humana obligatoria antes de ejecutar acciones.";
        return new RpmAnalisisDTO.MlPrediction(a.getPrioridadMl(),a.getConfianzaMl(),probs,a.getVersionModeloMl(),a.getMlEstado(),a.getMlGeneradoEn(),coincide,revisar,rec);
    }
    private String resumen(SoaControl s,int score,String pr,List<SignalSpec> specs){return s.getControl().getCodigo()+" presenta prioridad "+pr+" ("+score+"/100) con "+specs.size()+" señales activas.";}
    private String explicacion(SoaControl s,List<SignalSpec> specs,Riesgo r){String ant=specs.stream().filter(x->"ANTIGENO".equals(x.categoria)).map(SignalSpec::descripcion).collect(Collectors.joining(" "));String pel=specs.stream().filter(x->"PELIGRO".equals(x.categoria)).map(SignalSpec::descripcion).collect(Collectors.joining(" "));return "Representación: el control "+s.getControl().getCodigo()+" funciona como célula artificial dentro del tejido SoA-riesgos-evidencias. Detección/APC: "+(ant.isBlank()?"no se detectaron antígenos relevantes.":ant)+" Propagación de peligro: "+(pel.isBlank()?"no se identificaron consecuencias adicionales activas.":pel)+(r!=null?" Riesgo principal relacionado: "+r.getCodigo()+" ("+RiesgoService.categoria(r.getNivelInherente())+").":"")+" Reacción: las decisiones propuestas requieren validación humana (función Th) antes de ejecutarse y sus resultados pueden almacenarse como memoria RPM.";}
    private String buildInput(SoaControl s,List<Evidencia> e,List<RiesgoControl> r,List<HallazgoAuditoria> h,List<Capacitacion> c,List<SignalSpec> specs){return s.getId()+"|"+s.getAplicabilidad()+"|"+s.getEstadoImplementacion()+"|"+s.getPorcentajeImplementacion()+"|"+s.getFechaObjetivo()+"|"+e.stream().map(x->x.getId()+":"+x.getEstado()+":"+x.getFechaVencimiento()).sorted().toList()+"|"+r.stream().map(x->x.getRiesgo().getId()+":"+x.getRiesgo().getNivelInherente()+":"+x.getRiesgo().getEstado()).sorted().toList()+"|"+h.stream().map(x->x.getId()+":"+x.getSeveridad()+":"+x.getEstado()+":"+x.getRecurrente()).sorted().toList()+"|"+c.stream().map(x->x.getId()+":"+x.getEstado()).sorted().toList()+"|"+specs.stream().map(x->x.codigo+":"+x.valor).sorted().toList();}
    private String huellaSituacion(RpmAnalisis a,List<RpmSenal> ss){return sha256((a.getSoaControl()!=null?a.getSoaControl().getControl().getDominio():"")+"|"+ss.stream().map(RpmSenal::getCodigo).sorted().collect(Collectors.joining(",")));}
    private Set<String> tags(ControlCatalogo c){if(c.getEtiquetas()==null)return Set.of();return Arrays.stream(c.getEtiquetas().split(",")).map(String::trim).map(String::toLowerCase).collect(Collectors.toSet());}
    private void add(List<SignalSpec> l,String cat,String code,String desc,String source,int weight,String value){l.add(new SignalSpec(cat,code,desc,source,weight,value));}
    private String sha256(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
    private String trim(String s){return s==null?null:s.trim();}
    private record SignalSpec(String categoria,String codigo,String descripcion,String fuente,int peso,String valor){}
    private record Resultado(RpmAnalisis analisis,boolean reutilizado){}
}
