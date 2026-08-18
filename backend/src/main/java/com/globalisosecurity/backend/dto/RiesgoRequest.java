package com.globalisosecurity.backend.dto;
import java.time.LocalDate;
public record RiesgoRequest(Long servicioId,String codigo,String nombre,String activoInformacion,String amenaza,String vulnerabilidad,String consecuencia,Integer probabilidad,Integer impacto,Integer nivelResidual,String tratamiento,String responsable,String estado,LocalDate fechaRevision,String descripcion){}
