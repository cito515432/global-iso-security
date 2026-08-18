package com.globalisosecurity.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @TableGenerator(
            name = "rol_table_gen",
            table = "legacy_id_generators",
            pkColumnName = "entity_name",
            valueColumnName = "next_val",
            pkColumnValue = "roles",
            initialValue = 1000000,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "rol_table_gen")
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Boolean activo = true;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String permisos;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public String getPermisos() {
        return permisos;
    }

    public void setPermisos(String permisos) {
        this.permisos = permisos;
    }
}
