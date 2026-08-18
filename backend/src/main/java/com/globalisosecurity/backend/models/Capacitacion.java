package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "capacitaciones")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Capacitacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Column(length = 2000)
    private String objetivo;

    @Column(name = "material_url", length = 1000)
    private String materialUrl;

    @Column(name = "video_url", length = 1000)
    private String videoUrl;

    @Column(nullable = false)
    private String estado;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    @Column(name = "puntaje_minimo", nullable = false)
    private Integer puntajeMinimo = 80;

    @Column(name = "publico_objetivo", length = 1000)
    private String publicoObjetivo;

    @Column(name = "creada_por_rpm", nullable = false)
    private Boolean creadaPorRpm = false;

    @Column(name = "motivo_rpm", length = 2000)
    private String motivoRpm;

    @Column(name = "control_codigo", length = 30)
    private String controlCodigo;

    @Column(name = "riesgo_id_referencia")
    private Long riesgoIdReferencia;

    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }
    public String getMaterialUrl() { return materialUrl; }
    public void setMaterialUrl(String materialUrl) { this.materialUrl = materialUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDateTime fechaInicio) { this.fechaInicio = fechaInicio; }
    public LocalDateTime getFechaLimite() { return fechaLimite; }
    public void setFechaLimite(LocalDateTime fechaLimite) { this.fechaLimite = fechaLimite; }
    public LocalDateTime getFechaFinalizacion() { return fechaFinalizacion; }
    public void setFechaFinalizacion(LocalDateTime fechaFinalizacion) { this.fechaFinalizacion = fechaFinalizacion; }
    public Integer getPuntajeMinimo() { return puntajeMinimo; }
    public void setPuntajeMinimo(Integer puntajeMinimo) { this.puntajeMinimo = puntajeMinimo; }
    public String getPublicoObjetivo() { return publicoObjetivo; }
    public void setPublicoObjetivo(String publicoObjetivo) { this.publicoObjetivo = publicoObjetivo; }
    public Boolean getCreadaPorRpm() { return creadaPorRpm; }
    public void setCreadaPorRpm(Boolean creadaPorRpm) { this.creadaPorRpm = creadaPorRpm; }
    public String getMotivoRpm() { return motivoRpm; }
    public void setMotivoRpm(String motivoRpm) { this.motivoRpm = motivoRpm; }
    public String getControlCodigo() { return controlCodigo; }
    public void setControlCodigo(String controlCodigo) { this.controlCodigo = controlCodigo; }
    public Long getRiesgoIdReferencia() { return riesgoIdReferencia; }
    public void setRiesgoIdReferencia(Long riesgoIdReferencia) { this.riesgoIdReferencia = riesgoIdReferencia; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }
}
