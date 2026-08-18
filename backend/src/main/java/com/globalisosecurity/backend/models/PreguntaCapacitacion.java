package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "preguntas_capacitacion")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PreguntaCapacitacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "capacitacion_id", nullable = false)
    @JsonIgnore
    private Capacitacion capacitacion;

    @Column(nullable = false, length = 1200)
    private String enunciado;

    @Column(name = "opcion_a", nullable = false, length = 700)
    private String opcionA;

    @Column(name = "opcion_b", nullable = false, length = 700)
    private String opcionB;

    @Column(name = "opcion_c", length = 700)
    private String opcionC;

    @Column(name = "opcion_d", length = 700)
    private String opcionD;

    @Column(name = "respuesta_correcta", nullable = false, length = 1)
    private String respuestaCorrecta;

    @Column(length = 1500)
    private String explicacion;

    @Column(nullable = false)
    private Integer puntos = 1;

    @Column(name = "orden_pregunta", nullable = false)
    private Integer orden = 1;

    @Column(nullable = false)
    private Boolean activa = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Capacitacion getCapacitacion() { return capacitacion; }
    public void setCapacitacion(Capacitacion capacitacion) { this.capacitacion = capacitacion; }
    public String getEnunciado() { return enunciado; }
    public void setEnunciado(String enunciado) { this.enunciado = enunciado; }
    public String getOpcionA() { return opcionA; }
    public void setOpcionA(String opcionA) { this.opcionA = opcionA; }
    public String getOpcionB() { return opcionB; }
    public void setOpcionB(String opcionB) { this.opcionB = opcionB; }
    public String getOpcionC() { return opcionC; }
    public void setOpcionC(String opcionC) { this.opcionC = opcionC; }
    public String getOpcionD() { return opcionD; }
    public void setOpcionD(String opcionD) { this.opcionD = opcionD; }
    public String getRespuestaCorrecta() { return respuestaCorrecta; }
    public void setRespuestaCorrecta(String respuestaCorrecta) { this.respuestaCorrecta = respuestaCorrecta; }
    public String getExplicacion() { return explicacion; }
    public void setExplicacion(String explicacion) { this.explicacion = explicacion; }
    public Integer getPuntos() { return puntos; }
    public void setPuntos(Integer puntos) { this.puntos = puntos; }
    public Integer getOrden() { return orden; }
    public void setOrden(Integer orden) { this.orden = orden; }
    public Boolean getActiva() { return activa; }
    public void setActiva(Boolean activa) { this.activa = activa; }
}
