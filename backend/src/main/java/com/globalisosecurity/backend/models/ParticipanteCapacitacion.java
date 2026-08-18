package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "participantes_capacitacion", uniqueConstraints = @UniqueConstraint(columnNames = {"capacitacion_id", "email"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ParticipanteCapacitacion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "capacitacion_id", nullable = false)
    private Capacitacion capacitacion;
    @Column(nullable = false)
    private String nombre;
    @Column(nullable = false)
    private String email;
    private String documento;
    private String cargo;
    @Column(nullable = false, length = 30)
    private String estado = "ASIGNADO";
    @Column(name = "progreso_porcentaje", nullable = false)
    private Integer progresoPorcentaje = 0;
    @Column(name = "puntaje_evaluacion")
    private Double puntajeEvaluacion;
    @Column(nullable = false)
    private Integer intentos = 0;
    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion = LocalDateTime.now();
    @Column(name = "fecha_finalizacion")
    private LocalDateTime fechaFinalizacion;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Capacitacion getCapacitacion(){return capacitacion;} public void setCapacitacion(Capacitacion v){capacitacion=v;}
    public String getNombre(){return nombre;} public void setNombre(String v){nombre=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getDocumento(){return documento;} public void setDocumento(String v){documento=v;}
    public String getCargo(){return cargo;} public void setCargo(String v){cargo=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public Integer getProgresoPorcentaje(){return progresoPorcentaje;} public void setProgresoPorcentaje(Integer v){progresoPorcentaje=v;}
    public Double getPuntajeEvaluacion(){return puntajeEvaluacion;} public void setPuntajeEvaluacion(Double v){puntajeEvaluacion=v;}
    public Integer getIntentos(){return intentos;} public void setIntentos(Integer v){intentos=v;}
    public LocalDateTime getFechaAsignacion(){return fechaAsignacion;} public void setFechaAsignacion(LocalDateTime v){fechaAsignacion=v;}
    public LocalDateTime getFechaFinalizacion(){return fechaFinalizacion;} public void setFechaFinalizacion(LocalDateTime v){fechaFinalizacion=v;}
}
