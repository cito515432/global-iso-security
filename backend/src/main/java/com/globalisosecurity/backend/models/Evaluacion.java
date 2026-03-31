/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluaciones")
public class Evaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String resultadoGeneral;

    @Column(length = 2000)
    private String comentarios;

    @Column(name = "fecha_evaluacion")
    private LocalDateTime fechaEvaluacion;

    @Column(nullable = false)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    public Long getId() {
        return id;
    }

    public String getResultadoGeneral() {
        return resultadoGeneral;
    }

    public String getComentarios() {
        return comentarios;
    }

    public LocalDateTime getFechaEvaluacion() {
        return fechaEvaluacion;
    }

    public String getEstado() {
        return estado;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setResultadoGeneral(String resultadoGeneral) {
        this.resultadoGeneral = resultadoGeneral;
    }

    public void setComentarios(String comentarios) {
        this.comentarios = comentarios;
    }

    public void setFechaEvaluacion(LocalDateTime fechaEvaluacion) {
        this.fechaEvaluacion = fechaEvaluacion;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setServicio(Servicio servicio) {
        this.servicio = servicio;
    }
}