package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "modulos_capacitacion")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ModuloCapacitacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "capacitacion_id", nullable = false)
    private Capacitacion capacitacion;
    @Column(nullable = false, length = 500)
    private String titulo;
    @Column(length = 2000)
    private String descripcion;
    @Lob
    private String contenido;
    @Column(name = "material_url", length = 1000)
    private String materialUrl;
    @Column(name = "video_url", length = 1000)
    private String videoUrl;
    @Column(name = "orden_modulo", nullable = false)
    private Integer orden = 1;
    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos = 15;
    @Column(nullable = false)
    private Boolean obligatorio = true;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Capacitacion getCapacitacion(){return capacitacion;} public void setCapacitacion(Capacitacion v){capacitacion=v;}
    public String getTitulo(){return titulo;} public void setTitulo(String v){titulo=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getContenido(){return contenido;} public void setContenido(String v){contenido=v;}
    public String getMaterialUrl(){return materialUrl;} public void setMaterialUrl(String v){materialUrl=v;}
    public String getVideoUrl(){return videoUrl;} public void setVideoUrl(String v){videoUrl=v;}
    public Integer getOrden(){return orden;} public void setOrden(Integer v){orden=v;}
    public Integer getDuracionMinutos(){return duracionMinutos;} public void setDuracionMinutos(Integer v){duracionMinutos=v;}
    public Boolean getObligatorio(){return obligatorio;} public void setObligatorio(Boolean v){obligatorio=v;}
}
