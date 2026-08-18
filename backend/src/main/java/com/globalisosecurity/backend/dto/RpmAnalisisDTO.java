package com.globalisosecurity.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public record RpmAnalisisDTO(
        Long id,
        Long servicioId,
        Long soaControlId,
        String controlCodigo,
        String controlTitulo,
        Long riesgoId,
        String riesgoCodigo,
        LocalDateTime generadoEn,
        Integer puntaje,
        String prioridad,
        String estado,
        String resumen,
        String explicacion,
        String versionMotor,
        List<Senal> senales,
        List<Decision> decisiones,
        List<MemoriaSimilar> memoriaSimilar) {
    public record Senal(Long id,String categoria,String codigo,String descripcion,String fuente,Integer peso,String valor){}
    public record Decision(Long id,String tipoAccion,String accion,String estado,String validadaPor,LocalDateTime fechaValidacion,String justificacion,String fechaObjetivo){}
    public record MemoriaSimilar(Long id,String prioridadFinal,String accion,String resultado,Integer efectividadPorcentaje,LocalDateTime creadoEn){}
}
