/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "items_checklist")
public class ItemChecklist {

    @Id
    @TableGenerator(
            name = "item_checklist_table_gen",
            table = "legacy_id_generators",
            pkColumnName = "entity_name",
            valueColumnName = "next_val",
            pkColumnValue = "items_checklist",
            initialValue = 1000000,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "item_checklist_table_gen")
    private Long id;

    @Column(nullable = false, length = 1000)
    private String pregunta;

    @Column(length = 500)
    private String respuesta;

    @Column(length = 1000)
    private String observacion;

    @Column(nullable = false)
    private String estado;

    @ManyToOne
    @JoinColumn(name = "checklist_id", nullable = false)
    private Checklist checklist;

    public Long getId() {
        return id;
    }

    public String getPregunta() {
        return pregunta;
    }

    public String getRespuesta() {
        return respuesta;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getEstado() {
        return estado;
    }

    public Checklist getChecklist() {
        return checklist;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setPregunta(String pregunta) {
        this.pregunta = pregunta;
    }

    public void setRespuesta(String respuesta) {
        this.respuesta = respuesta;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public void setChecklist(Checklist checklist) {
        this.checklist = checklist;
    }
}
