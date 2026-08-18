package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

@Entity
@Table(name = "catalogo_controles")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ControlCatalogo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;
    @Column(nullable = false, length = 60)
    private String dominio;
    @Column(nullable = false, length = 500)
    private String titulo;
    @Column(length = 3000)
    private String descripcion;
    @Column(name = "pregunta_evaluacion", length = 3000)
    private String preguntaEvaluacion;
    @Column(length = 1000)
    private String etiquetas;
    @Column(name = "version_norma", nullable = false, length = 50)
    private String versionNorma = "ISO/IEC 27001:2022";
    @Column(nullable = false)
    private Boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getDominio() { return dominio; }
    public void setDominio(String dominio) { this.dominio = dominio; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getPreguntaEvaluacion() { return preguntaEvaluacion; }
    public void setPreguntaEvaluacion(String preguntaEvaluacion) { this.preguntaEvaluacion = preguntaEvaluacion; }
    public String getEtiquetas() { return etiquetas; }
    public void setEtiquetas(String etiquetas) { this.etiquetas = etiquetas; }
    public String getVersionNorma() { return versionNorma; }
    public void setVersionNorma(String versionNorma) { this.versionNorma = versionNorma; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
