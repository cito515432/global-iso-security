package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hallazgos_auditoria")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HallazgoAuditoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @ManyToOne @JoinColumn(name = "soa_control_id")
    private SoaControl soaControl;
    @ManyToOne @JoinColumn(name = "riesgo_id")
    private Riesgo riesgo;
    @Column(nullable = false, length = 500)
    private String titulo;
    @Column(nullable = false, length = 3000)
    private String descripcion;
    @Column(nullable = false, length = 30)
    private String severidad = "MEDIA";
    @Column(nullable = false, length = 30)
    private String estado = "ABIERTO";
    @Column(nullable = false)
    private Boolean recurrente = false;
    @Column(name = "fecha_deteccion", nullable = false)
    private LocalDateTime fechaDeteccion = LocalDateTime.now();
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;
    @Column(name = "creado_por", nullable = false)
    private String creadoPor;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Servicio getServicio(){return servicio;} public void setServicio(Servicio v){servicio=v;}
    public SoaControl getSoaControl(){return soaControl;} public void setSoaControl(SoaControl v){soaControl=v;}
    public Riesgo getRiesgo(){return riesgo;} public void setRiesgo(Riesgo v){riesgo=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getSeveridad(){return severidad;} public void setSeveridad(String v){severidad=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public Boolean getRecurrente(){return recurrente;} public void setRecurrente(Boolean v){recurrente=v;}
    public LocalDateTime getFechaDeteccion(){return fechaDeteccion;} public void setFechaDeteccion(LocalDateTime v){fechaDeteccion=v;}
    public LocalDateTime getFechaCierre(){return fechaCierre;} public void setFechaCierre(LocalDateTime v){fechaCierre=v;}
    public String getCreadoPor(){return creadoPor;} public void setCreadoPor(String v){creadoPor=v;}
}
