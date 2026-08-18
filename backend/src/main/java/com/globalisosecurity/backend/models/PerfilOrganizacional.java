package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "perfiles_organizacionales", uniqueConstraints = @UniqueConstraint(columnNames = "empresa_id"))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PerfilOrganizacional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;

    @ManyToOne
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(nullable = false)
    private String tamano = "PEQUENA";

    @Column(name = "maneja_datos_sensibles", nullable = false)
    private boolean manejaDatosSensibles;
    @Column(name = "usa_servicios_nube", nullable = false)
    private boolean usaServiciosNube;
    @Column(name = "permite_trabajo_remoto", nullable = false)
    private boolean permiteTrabajoRemoto;
    @Column(name = "procesa_pagos", nullable = false)
    private boolean procesaPagos;
    @Column(name = "infraestructura_propia", nullable = false)
    private boolean infraestructuraPropia;
    @Column(name = "depende_proveedores", nullable = false)
    private boolean dependeProveedores;
    @Column(name = "servicio_critico_24x7", nullable = false)
    private boolean servicioCritico24x7;
    @Column(name = "maneja_menores", nullable = false)
    private boolean manejaMenores;
    @Column(name = "opera_ot_iot", nullable = false)
    private boolean operaOtIot;

    @Column(name = "alcance_sgsi", length = 3000)
    private String alcanceSgsi;
    @Column(name = "responsable_sgsi")
    private String responsableSgsi;
    @Column(name = "umbral_aceptacion", nullable = false)
    private Integer umbralAceptacion = 8;
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Empresa getEmpresa() { return empresa; }
    public void setEmpresa(Empresa empresa) { this.empresa = empresa; }
    public Sector getSector() { return sector; }
    public void setSector(Sector sector) { this.sector = sector; }
    public String getTamano() { return tamano; }
    public void setTamano(String tamano) { this.tamano = tamano; }
    public boolean isManejaDatosSensibles() { return manejaDatosSensibles; }
    public void setManejaDatosSensibles(boolean v) { this.manejaDatosSensibles = v; }
    public boolean isUsaServiciosNube() { return usaServiciosNube; }
    public void setUsaServiciosNube(boolean v) { this.usaServiciosNube = v; }
    public boolean isPermiteTrabajoRemoto() { return permiteTrabajoRemoto; }
    public void setPermiteTrabajoRemoto(boolean v) { this.permiteTrabajoRemoto = v; }
    public boolean isProcesaPagos() { return procesaPagos; }
    public void setProcesaPagos(boolean v) { this.procesaPagos = v; }
    public boolean isInfraestructuraPropia() { return infraestructuraPropia; }
    public void setInfraestructuraPropia(boolean v) { this.infraestructuraPropia = v; }
    public boolean isDependeProveedores() { return dependeProveedores; }
    public void setDependeProveedores(boolean v) { this.dependeProveedores = v; }
    public boolean isServicioCritico24x7() { return servicioCritico24x7; }
    public void setServicioCritico24x7(boolean v) { this.servicioCritico24x7 = v; }
    public boolean isManejaMenores() { return manejaMenores; }
    public void setManejaMenores(boolean v) { this.manejaMenores = v; }
    public boolean isOperaOtIot() { return operaOtIot; }
    public void setOperaOtIot(boolean v) { this.operaOtIot = v; }
    public String getAlcanceSgsi() { return alcanceSgsi; }
    public void setAlcanceSgsi(String v) { this.alcanceSgsi = v; }
    public String getResponsableSgsi() { return responsableSgsi; }
    public void setResponsableSgsi(String v) { this.responsableSgsi = v; }
    public Integer getUmbralAceptacion() { return umbralAceptacion; }
    public void setUmbralAceptacion(Integer v) { this.umbralAceptacion = v; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime v) { this.actualizadoEn = v; }
}
