package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "intentos_capacitacion")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class IntentoCapacitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    @JsonIgnore
    private ParticipanteCapacitacion participante;

    @Column(name = "fecha_intento", nullable = false)
    private LocalDateTime fechaIntento = LocalDateTime.now();

    @Column(nullable = false)
    private Double puntaje;

    @Column(nullable = false)
    private Boolean aprobado;

    @Column(name = "respuestas_json", columnDefinition = "LONGTEXT")
    private String respuestasJson;

    @Column(name = "respuestas_correctas", nullable = false)
    private Integer respuestasCorrectas = 0;

    @Column(name = "total_preguntas", nullable = false)
    private Integer totalPreguntas = 0;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public ParticipanteCapacitacion getParticipante() { return participante; }
    public void setParticipante(ParticipanteCapacitacion participante) { this.participante = participante; }
    public LocalDateTime getFechaIntento() { return fechaIntento; }
    public void setFechaIntento(LocalDateTime fechaIntento) { this.fechaIntento = fechaIntento; }
    public Double getPuntaje() { return puntaje; }
    public void setPuntaje(Double puntaje) { this.puntaje = puntaje; }
    public Boolean getAprobado() { return aprobado; }
    public void setAprobado(Boolean aprobado) { this.aprobado = aprobado; }
    public String getRespuestasJson() { return respuestasJson; }
    public void setRespuestasJson(String respuestasJson) { this.respuestasJson = respuestasJson; }
    public Integer getRespuestasCorrectas() { return respuestasCorrectas; }
    public void setRespuestasCorrectas(Integer respuestasCorrectas) { this.respuestasCorrectas = respuestasCorrectas; }
    public Integer getTotalPreguntas() { return totalPreguntas; }
    public void setTotalPreguntas(Integer totalPreguntas) { this.totalPreguntas = totalPreguntas; }
}
