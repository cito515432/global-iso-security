package com.globalisosecurity.backend.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        MlPrediction ml,
        List<Senal> senales,
        List<Decision> decisiones,
        List<MemoriaSimilar> memoriaSimilar) {
    public record MlPrediction(
            String prioridad,
            Double confianza,
            Map<String,Double> probabilidades,
            String versionModelo,
            String estado,
            LocalDateTime generadoEn,
            Boolean coincideConRpm,
            Boolean requiereRevisionHumana,
            String recomendacion){}
    public record Senal(Long id,String categoria,String codigo,String descripcion,String fuente,Integer peso,String valor){}
    public record Decision(Long id,String tipoAccion,String accion,String estado,String validadaPor,LocalDateTime fechaValidacion,String justificacion,String fechaObjetivo){}
    public record MemoriaSimilar(Long id,String prioridadFinal,String accion,String resultado,Integer efectividadPorcentaje,LocalDateTime creadoEn){}
}
