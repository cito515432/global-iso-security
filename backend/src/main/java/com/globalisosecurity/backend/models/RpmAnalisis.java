package com.globalisosecurity.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "rpm_analisis")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class RpmAnalisis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;
    @ManyToOne @JoinColumn(name = "soa_control_id")
    private SoaControl soaControl;
    @ManyToOne @JoinColumn(name = "riesgo_id")
    private Riesgo riesgo;
    @Column(name = "generado_en", nullable = false)
    private LocalDateTime generadoEn = LocalDateTime.now();
    @Column(nullable = false)
    private Integer puntaje = 0;
    @Column(nullable = false, length = 20)
    private String prioridad = "BAJA";
    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE_VALIDACION";
    @Column(length = 1000)
    private String resumen;
    @Column(length = 5000)
    private String explicacion;
    @Column(name = "version_motor", nullable = false, length = 40)
    private String versionMotor = "RPM-DETERMINISTA-1.0";
    @Column(name = "huella_entrada", length = 64)
    private String huellaEntrada;
    @Column(name = "prioridad_ml", length = 20)
    private String prioridadMl;
    @Column(name = "confianza_ml")
    private Double confianzaMl;
    @Column(name = "probabilidades_ml", columnDefinition = "TEXT")
    private String probabilidadesMl;
    @Column(name = "version_modelo_ml", length = 80)
    private String versionModeloMl;
    @Column(name = "ml_estado", length = 30)
    private String mlEstado = "PENDIENTE";
    @Column(name = "ml_generado_en")
    private LocalDateTime mlGeneradoEn;

    public Long getId(){return id;} public void setId(Long v){id=v;}
    public Servicio getServicio(){return servicio;} public void setServicio(Servicio v){servicio=v;}
    public SoaControl getSoaControl(){return soaControl;} public void setSoaControl(SoaControl v){soaControl=v;}
    public Riesgo getRiesgo(){return riesgo;} public void setRiesgo(Riesgo v){riesgo=v;}
    public LocalDateTime getGeneradoEn(){return generadoEn;} public void setGeneradoEn(LocalDateTime v){generadoEn=v;}
    public Integer getPuntaje(){return puntaje;} public void setPuntaje(Integer v){puntaje=v;}
    public String getPrioridad(){return prioridad;} public void setPrioridad(String v){prioridad=v;}
    public String getEstado(){return estado;} public void setEstado(String v){estado=v;}
    public String getResumen(){return resumen;} public void setResumen(String v){resumen=v;}
    public String getExplicacion(){return explicacion;} public void setExplicacion(String v){explicacion=v;}
    public String getVersionMotor(){return versionMotor;} public void setVersionMotor(String v){versionMotor=v;}
    public String getHuellaEntrada(){return huellaEntrada;} public void setHuellaEntrada(String v){huellaEntrada=v;}
    public String getPrioridadMl(){return prioridadMl;} public void setPrioridadMl(String v){prioridadMl=v;}
    public Double getConfianzaMl(){return confianzaMl;} public void setConfianzaMl(Double v){confianzaMl=v;}
    public String getProbabilidadesMl(){return probabilidadesMl;} public void setProbabilidadesMl(String v){probabilidadesMl=v;}
    public String getVersionModeloMl(){return versionModeloMl;} public void setVersionModeloMl(String v){versionModeloMl=v;}
    public String getMlEstado(){return mlEstado;} public void setMlEstado(String v){mlEstado=v;}
    public LocalDateTime getMlGeneradoEn(){return mlGeneradoEn;} public void setMlGeneradoEn(LocalDateTime v){mlGeneradoEn=v;}
}
