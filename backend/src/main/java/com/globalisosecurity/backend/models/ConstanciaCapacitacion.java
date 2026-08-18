package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "constancias_capacitacion", uniqueConstraints = {
    @UniqueConstraint(name = "uk_constancia_participante", columnNames = "participante_id"),
    @UniqueConstraint(name = "uk_constancia_codigo", columnNames = "codigo_verificacion")
})
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ConstanciaCapacitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false)
    private String nombreCompleto;

    @Column(nullable = false)
    private String documento;

    @Column(name = "codigo_interno")
    private String codigoInterno;

    @Column(nullable = false)
    private String cargo;

    @Column(name = "fecha_firma", nullable = false)
    private LocalDateTime fechaFirma;

    @Column(name = "codigo_verificacion", nullable = false, length = 80)
    private String codigoVerificacion;

    @Column
    private Double puntaje;

    @Column(nullable = false, length = 30)
    private String estado = "VIGENTE";

    @ManyToOne
    @JoinColumn(name = "capacitacion_id", nullable = false)
    private Capacitacion capacitacion;

    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @OneToOne
    @JoinColumn(name = "participante_id")
    private ParticipanteCapacitacion participante;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String codigoInterno) { this.codigoInterno = codigoInterno; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public LocalDateTime getFechaFirma() { return fechaFirma; }
    public void setFechaFirma(LocalDateTime fechaFirma) { this.fechaFirma = fechaFirma; }
    public String getCodigoVerificacion() { return codigoVerificacion; }
    public void setCodigoVerificacion(String codigoVerificacion) { this.codigoVerificacion = codigoVerificacion; }
    public Double getPuntaje() { return puntaje; }
    public void setPuntaje(Double puntaje) { this.puntaje = puntaje; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public Capacitacion getCapacitacion() { return capacitacion; }
    public void setCapacitacion(Capacitacion capacitacion) { this.capacitacion = capacitacion; }
    public Servicio getServicio() { return servicio; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }
    public ParticipanteCapacitacion getParticipante() { return participante; }
    public void setParticipante(ParticipanteCapacitacion participante) { this.participante = participante; }
}
