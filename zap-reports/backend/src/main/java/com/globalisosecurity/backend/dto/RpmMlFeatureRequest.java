package com.globalisosecurity.backend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RpmMlFeatureRequest(
        @JsonProperty("analysis_id") Long analysisId,
        String sector,
        String tamano,
        @JsonProperty("control_dominio") String controlDominio,
        @JsonProperty("control_humano") Integer controlHumano,
        String aplicabilidad,
        @JsonProperty("estado_implementacion") String estadoImplementacion,
        @JsonProperty("porcentaje_implementacion") Integer porcentajeImplementacion,
        @JsonProperty("puntaje_relevancia") Integer puntajeRelevancia,
        @JsonProperty("fecha_objetivo_vencida") Integer fechaObjetivoVencida,
        Integer probabilidad,
        Integer impacto,
        @JsonProperty("nivel_inherente") Integer nivelInherente,
        @JsonProperty("riesgo_categoria") String riesgoCategoria,
        @JsonProperty("evidencias_total") Integer evidenciasTotal,
        @JsonProperty("evidencias_pendientes") Integer evidenciasPendientes,
        @JsonProperty("evidencias_rechazadas") Integer evidenciasRechazadas,
        @JsonProperty("evidencias_vencidas") Integer evidenciasVencidas,
        @JsonProperty("hallazgos_abiertos") Integer hallazgosAbiertos,
        @JsonProperty("hallazgos_recurrentes") Integer hallazgosRecurrentes,
        @JsonProperty("hallazgo_severidad_ordinal") Integer hallazgoSeveridadOrdinal) {
}
