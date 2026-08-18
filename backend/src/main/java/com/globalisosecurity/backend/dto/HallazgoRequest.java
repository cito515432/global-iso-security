package com.globalisosecurity.backend.dto;
public record HallazgoRequest(Long servicioId,Long soaControlId,Long riesgoId,String titulo,String descripcion,String severidad,String estado,Boolean recurrente){}
