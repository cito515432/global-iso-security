package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "soa_controles", uniqueConstraints = @UniqueConstraint(columnNames = {"servicio_id", "control_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SoaControl {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @ManyToOne(optional = false) @JoinColumn(name = "control_id", nullable = false)
    private ControlCatalogo control;
    @Column(nullable = false, length = 30)
    private String aplicabilidad = "PENDIENTE";
    @Column(name = "justificacion_aplicabilidad", length = 3000)
    private String justificacionAplicabilidad;
    @Column(name = "estado_implementacion", nullable = false, length = 30)
    private String estadoImplementacion = "NO_INICIADO";
    @Column(name = "porcentaje_implementacion", nullable = false)
    private Integer porcentajeImplementacion = 0;
    private String responsable;
    @Column(name = "fecha_objetivo")
    private LocalDate fechaObjetivo;
    @Column(length = 3000)
    private String observaciones;
    @Column(name = "recomendacion_contextual", length = 1500)
    private String recomendacionContextual;
    @Column(name = "puntaje_relevancia", nullable = false)
    private Integer puntajeRelevancia = 0;
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();
    @Version
    private Long version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }
    public ControlCatalogo getControl() { return control; }
    public void setControl(ControlCatalogo control) { this.control = control; }
    public String getAplicabilidad() { return aplicabilidad; }
    public void setAplicabilidad(String v) { this.aplicabilidad = v; }
    public String getJustificacionAplicabilidad() { return justificacionAplicabilidad; }
    public void setJustificacionAplicabilidad(String v) { this.justificacionAplicabilidad = v; }
    public String getEstadoImplementacion() { return estadoImplementacion; }
    public void setEstadoImplementacion(String v) { this.estadoImplementacion = v; }
    public Integer getPorcentajeImplementacion() { return porcentajeImplementacion; }
    public void setPorcentajeImplementacion(Integer v) { this.porcentajeImplementacion = v; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String v) { this.responsable = v; }
    public LocalDate getFechaObjetivo() { return fechaObjetivo; }
    public void setFechaObjetivo(LocalDate v) { this.fechaObjetivo = v; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { this.observaciones = v; }
    public String getRecomendacionContextual() { return recomendacionContextual; }
    public void setRecomendacionContextual(String v) { this.recomendacionContextual = v; }
    public Integer getPuntajeRelevancia() { return puntajeRelevancia; }
    public void setPuntajeRelevancia(Integer v) { this.puntajeRelevancia = v; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime v) { this.creadoEn = v; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime v) { this.actualizadoEn = v; }
    public Long getVersion() { return version; }
}
