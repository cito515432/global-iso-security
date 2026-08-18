package com.globalisosecurity.backend.dto;
public record PreguntaCapacitacionRequest(String enunciado,String opcionA,String opcionB,String opcionC,String opcionD,String respuestaCorrecta,String explicacion,Integer puntos,Integer orden,Boolean activa){}
