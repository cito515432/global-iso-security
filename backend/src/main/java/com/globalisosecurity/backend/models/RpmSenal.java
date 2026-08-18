package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "rpm_senales")
public class RpmSenal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "analisis_id", nullable = false)
    @JsonIgnore
    private RpmAnalisis analisis;
    @Column(nullable = false, length = 30)
    private String categoria;
    @Column(nullable = false, length = 80)
    private String codigo;
    @Column(nullable = false, length = 1500)
    private String descripcion;
    @Column(length = 500)
    private String fuente;
    @Column(nullable = false)
    private Integer peso;
    @Column(length = 500)
    private String valor;
    @Column(nullable = false)
    private Boolean activa = true;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public RpmAnalisis getAnalisis(){return analisis;} public void setAnalisis(RpmAnalisis v){analisis=v;}
    public String getCategoria(){return categoria;} public void setCategoria(String v){categoria=v;}
    public String getCodigo(){return codigo;} public void setCodigo(String v){codigo=v;}
    public String getDescripcion(){return descripcion;} public void setDescripcion(String v){descripcion=v;}
    public String getFuente(){return fuente;} public void setFuente(String v){fuente=v;}
    public Integer getPeso(){return peso;} public void setPeso(Integer v){peso=v;}
    public String getValor(){return valor;} public void setValor(String v){valor=v;}
    public Boolean getActiva(){return activa;} public void setActiva(Boolean v){activa=v;}
}
