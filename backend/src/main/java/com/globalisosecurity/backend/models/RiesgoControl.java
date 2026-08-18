package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "riesgos_controles", uniqueConstraints = @UniqueConstraint(columnNames = {"riesgo_id", "control_id"}))
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RiesgoControl {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "riesgo_id", nullable = false)
    private Riesgo riesgo;
    @ManyToOne(optional = false) @JoinColumn(name = "control_id", nullable = false)
    private ControlCatalogo control;
    @Column(name = "tipo_relacion", nullable = false, length = 40)
    private String tipoRelacion = "TRATAMIENTO";
    @Column(name = "eficacia_esperada", nullable = false)
    private Integer eficaciaEsperada = 50;
    @Column(length = 1500)
    private String observacion;

    public Long getId() { return id; }
    public void setId(Long v) { id = v; }
    public Riesgo getRiesgo() { return riesgo; }
    public void setRiesgo(Riesgo v) { riesgo = v; }
    public ControlCatalogo getControl() { return control; }
    public void setControl(ControlCatalogo v) { control = v; }
    public String getTipoRelacion() { return tipoRelacion; }
    public void setTipoRelacion(String v) { tipoRelacion = v; }
    public Integer getEficaciaEsperada() { return eficaciaEsperada; }
    public void setEficaciaEsperada(Integer v) { eficaciaEsperada = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { observacion = v; }
}
