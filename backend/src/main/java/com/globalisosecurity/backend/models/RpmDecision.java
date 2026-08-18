package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rpm_decisiones")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RpmDecision {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "analisis_id", nullable = false)
    private RpmAnalisis analisis;
    @Column(name = "tipo_accion", nullable = false, length = 50)
    private String tipoAccion;
    @Column(nullable = false, length = 3000)
    private String accion;
    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";
    @Column(name = "validada_por")
    private String validadaPor;
    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;
    @Column(length = 3000)
    private String justificacion;
    @Column(name = "fecha_objetivo")
    private LocalDate fechaObjetivo;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public RpmAnalisis getAnalisis(){return analisis;} public void setAnalisis(RpmAnalisis v){analisis=v;}
    public String getTipoAccion(){return tipoAccion;} public void setTipoAccion(String v){tipoAccion=v;}
    public String getAccion(){return accion;} public void setAccion(String v){accion=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public String getValidadaPor(){return validadaPor;} public void setValidadaPor(String v){validadaPor=v;}
    public LocalDateTime getFechaValidacion(){return fechaValidacion;} public void setFechaValidacion(LocalDateTime v){fechaValidacion=v;}
    public String getJustificacion(){return justificacion;} public void setJustificacion(String v){justificacion=v;}
    public LocalDate getFechaObjetivo(){return fechaObjetivo;} public void setFechaObjetivo(LocalDate v){fechaObjetivo=v;}
}
