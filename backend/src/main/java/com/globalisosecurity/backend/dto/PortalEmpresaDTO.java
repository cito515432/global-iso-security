package com.globalisosecurity.backend.dto;

import java.util.List;

public record PortalEmpresaDTO(
        Long empresaId,
        String empresaNombre,
        Long servicioId,
        String sector,
        String estadoServicio,
        int progresoGeneral,
        Etapas etapas,
        SoaResumen soa,
        RiesgosResumen riesgos,
        EvidenciasResumen evidencias,
        HallazgosResumen hallazgos,
        RpmResumen rpm,
        CapacitacionResumen capacitacion,
        List<RpmItem> prioridades,
        List<Actividad> actividadesPendientes) {
    public record Etapas(int contexto,int riesgos,int soa,int implementacion,int auditoria,int cierre){}
    public record SoaResumen(long total,long aplicables,long noAplicables,long pendientes,long implementados,long parciales,long noIniciados,int porcentaje){}
    public record RiesgosResumen(long total,long criticos,long altos,long medios,long bajos,long abiertos){}
    public record EvidenciasResumen(long total,long validadas,long pendientes,long rechazadas,long vencidas){}
    public record HallazgosResumen(long abiertos,long recurrentes,long criticos){}
    public record RpmResumen(long alertasActivas,long pendientesValidacion,long memoriaCasos){}
    public record CapacitacionResumen(long programas,long participantes,int finalizacionPromedio,int aprobacionPromedio){}
    public record RpmItem(Long analisisId,String controlCodigo,String controlTitulo,Integer puntaje,String prioridad,String resumen){}
    public record Actividad(String tipo,String titulo,String detalle,String prioridad){}
}
