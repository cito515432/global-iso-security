/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

package com.globalisosecurity.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "empresas")
public class Empresa {

    @Id
    @TableGenerator(
            name = "empresa_table_gen",
            table = "legacy_id_generators",
            pkColumnName = "entity_name",
            valueColumnName = "next_val",
            pkColumnValue = "empresas",
            initialValue = 1000000,
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "empresa_table_gen")
    private Long id;

    @Column(nullable = false)
    private String nombre;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
