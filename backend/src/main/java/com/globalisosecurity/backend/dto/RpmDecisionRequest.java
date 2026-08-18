package com.globalisosecurity.backend.dto;
import java.time.LocalDate;
public record RpmDecisionRequest(String estado,String justificacion,String accion,String tipoAccion,LocalDate fechaObjetivo){}
