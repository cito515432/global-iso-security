/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.dto;

public class UsuarioMeResponse {

    private Long id;
    private String nombre;
    private String email;
    private String rol;
    private String permisos;
    private EmpresaResumen empresa;

    public static class EmpresaResumen {
        private Long id;
        private String nombre;

        public EmpresaResumen() {
        }

        public EmpresaResumen(Long id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

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
    }

    public UsuarioMeResponse() {
    }

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRol() { return rol; }

    public void setRol(String rol) { this.rol = rol; }

    public String getPermisos() { return permisos; }

    public void setPermisos(String permisos) { this.permisos = permisos; }

    public EmpresaResumen getEmpresa() {
        return empresa;
    }

    public void setEmpresa(EmpresaResumen empresa) {
        this.empresa = empresa;
    }
}