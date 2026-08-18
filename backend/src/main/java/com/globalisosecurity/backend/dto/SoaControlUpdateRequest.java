package com.globalisosecurity.backend.dto;
import java.time.LocalDate;
public record SoaControlUpdateRequest(String aplicabilidad,String justificacionAplicabilidad,String estadoImplementacion,Integer porcentajeImplementacion,String responsable,LocalDate fechaObjetivo,String observaciones){}
