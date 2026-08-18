package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rpm_memoria")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RpmMemoria {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "analisis_id", nullable = false)
    private RpmAnalisis analisis;
    @Column(nullable = false, length = 64)
    private String huella;
    @Lob @Column(name = "situacion_json")
    private String situacionJson;
    @Column(name = "prioridad_inicial", length = 20)
    private String prioridadInicial;
    @Column(name = "prioridad_final", length = 20)
    private String prioridadFinal;
    @Column(length = 3000)
    private String accion;
    @Column(length = 3000)
    private String resultado;
    @Column(name = "efectividad_porcentaje")
    private Integer efectividadPorcentaje;
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public RpmAnalisis getAnalisis(){return analisis;} public void setAnalisis(RpmAnalisis v){analisis=v;}
    public String getHuella(){return huella;} public void setHuella(String v){huella=v;}
    public String getSituacionJson(){return situacionJson;} public void setSituacionJson(String v){situacionJson=v;}
    public String getPrioridadInicial(){return prioridadInicial;} public void setPrioridadInicial(String v){prioridadInicial=v;}
    public String getPrioridadFinal(){return prioridadFinal;} public void setPrioridadFinal(String v){prioridadFinal=v;}
    public String getAccion(){return accion;} public void setAccion(String v){accion=v;}
    public String getResultado(){return resultado;} public void setResultado(String v){resultado=v;}
    public Integer getEfectividadPorcentaje(){return efectividadPorcentaje;} public void setEfectividadPorcentaje(Integer v){efectividadPorcentaje=v;}
    public LocalDateTime getCreadoEn(){return creadoEn;} public void setCreadoEn(LocalDateTime v){creadoEn=v;}
}
