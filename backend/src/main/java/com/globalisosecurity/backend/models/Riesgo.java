package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "riesgos", uniqueConstraints = @UniqueConstraint(columnNames = {"servicio_id", "codigo"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Riesgo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @Column(nullable = false, length = 40)
    private String codigo;
    @Column(nullable = false, length = 500)
    private String nombre;
    @Column(name = "activo_informacion", length = 1000)
    private String activoInformacion;
    @Column(length = 1500)
    private String amenaza;
    @Column(length = 1500)
    private String vulnerabilidad;
    @Column(length = 2000)
    private String consecuencia;
    @Column(nullable = false)
    private Integer probabilidad = 1;
    @Column(nullable = false)
    private Integer impacto = 1;
    @Column(name = "nivel_inherente", nullable = false)
    private Integer nivelInherente = 1;
    @Column(name = "nivel_residual")
    private Integer nivelResidual;
    @Column(length = 50)
    private String tratamiento = "MITIGAR";
    @Column(length = 255)
    private String responsable;
    @Column(nullable = false, length = 30)
    private String estado = "ABIERTO";
    @Column(name = "fecha_revision")
    private LocalDate fechaRevision;
    @Column(length = 3000)
    private String descripcion;
    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn = LocalDateTime.now();
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio v) { this.servicio = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { this.codigo = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = v; }
    public String getActivoInformacion() { return activoInformacion; }
    public void setActivoInformacion(String v) { this.activoInformacion = v; }
    public String getAmenaza() { return amenaza; }
    public void setAmenaza(String v) { this.amenaza = v; }
    public String getVulnerabilidad() { return vulnerabilidad; }
    public void setVulnerabilidad(String v) { this.vulnerabilidad = v; }
    public String getConsecuencia() { return consecuencia; }
    public void setConsecuencia(String v) { this.consecuencia = v; }
    public Integer getProbabilidad() { return probabilidad; }
    public void setProbabilidad(Integer v) { this.probabilidad = v; }
    public Integer getImpacto() { return impacto; }
    public void setImpacto(Integer v) { this.impacto = v; }
    public Integer getNivelInherente() { return nivelInherente; }
    public void setNivelInherente(Integer v) { this.nivelInherente = v; }
    public Integer getNivelResidual() { return nivelResidual; }
    public void setNivelResidual(Integer v) { this.nivelResidual = v; }
    public String getTratamiento() { return tratamiento; }
    public void setTratamiento(String v) { this.tratamiento = v; }
    public String getResponsable() { return responsable; }
    public void setResponsable(String v) { this.responsable = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }
    public LocalDate getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(LocalDate v) { this.fechaRevision = v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime v) { this.creadoEn = v; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime v) { this.actualizadoEn = v; }
}
