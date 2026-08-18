package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "evidencias")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Evidencia {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @ManyToOne(optional = false) @JoinColumn(name = "soa_control_id", nullable = false)
    private SoaControl soaControl;
    @Column(name = "nombre_original", nullable = false, length = 500)
    private String nombreOriginal;
    @Column(name = "nombre_almacenado", nullable = false, length = 500)
    private String nombreAlmacenado;
    @Column(name = "ruta_archivo", nullable = false, length = 1500)
    private String rutaArchivo;
    @Column(name = "tipo_mime", length = 255)
    private String tipoMime;
    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;
    @Column(length = 2000)
    private String descripcion;
    @Column(name = "tipo_evidencia", length = 80)
    private String tipoEvidencia;
    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga = LocalDateTime.now();
    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;
    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";
    @Column(name = "cargada_por", nullable = false)
    private String cargadaPor;
    @Column(name = "validada_por")
    private String validadaPor;
    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;
    @Column(name = "observacion_validacion", length = 2000)
    private String observacionValidacion;

    public Long getId() { return id; }
    public void setId(Long v) { id=v; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio v) { servicio=v; }
    public SoaControl getSoaControl() { return soaControl; }
    public void setSoaControl(SoaControl v) { soaControl=v; }
    public String getNombreOriginal() { return nombreOriginal; }
    public void setNombreOriginal(String v) { nombreOriginal=v; }
    public String getNombreAlmacenado() { return nombreAlmacenado; }
    public void setNombreAlmacenado(String v) { nombreAlmacenado=v; }
    public String getRutaArchivo() { return rutaArchivo; }
    public void setRutaArchivo(String v) { rutaArchivo=v; }
    public String getTipoMime() { return tipoMime; }
    public void setTipoMime(String v) { tipoMime=v; }
    public String getHashSha256() { return hashSha256; }
    public void setHashSha256(String v) { hashSha256=v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion=v; }
    public String getTipoEvidencia() { return tipoEvidencia; }
    public void setTipoEvidencia(String v) { tipoEvidencia=v; }
    public LocalDateTime getFechaCarga() { return fechaCarga; }
    public void setFechaCarga(LocalDateTime v) { fechaCarga=v; }
    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate v) { fechaVencimiento=v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado=v; }
    public String getCargadaPor() { return cargadaPor; }
    public void setCargadaPor(String v) { cargadaPor=v; }
    public String getValidadaPor() { return validadaPor; }
    public void setValidadaPor(String v) { validadaPor=v; }
    public LocalDateTime getFechaValidacion() { return fechaValidacion; }
    public void setFechaValidacion(LocalDateTime v) { fechaValidacion=v; }
    public String getObservacionValidacion() { return observacionValidacion; }
    public void setObservacionValidacion(String v) { observacionValidacion=v; }
}
