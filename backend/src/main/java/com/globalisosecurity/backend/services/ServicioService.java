/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ServicioService {

    @Autowired
    private ServicioRepository servicioRepository;

    public List<Servicio> obtenerTodos() {
        return servicioRepository.findAll();
    }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicioRepository.findById(id);
    }

    public List<Servicio> obtenerPorEstado(String estado) {
        return servicioRepository.findByEstado(estado);
    }

    public Servicio crearServicio(Servicio servicio) {
        if (servicio.getFechaCreacion() == null) {
            servicio.setFechaCreacion(LocalDateTime.now());
        }

        if (servicio.getEstado() == null || servicio.getEstado().isBlank()) {
            servicio.setEstado("BORRADOR");
        }

        return servicioRepository.save(servicio);
    }

    public Servicio actualizarServicio(Long id, Servicio servicioActualizado) {
        Optional<Servicio> servicio = servicioRepository.findById(id);

        if (servicio.isPresent()) {
            Servicio s = servicio.get();
            s.setEmpresa(servicioActualizado.getEmpresa());
            s.setSector(servicioActualizado.getSector());
            s.setEstado(servicioActualizado.getEstado());
            return servicioRepository.save(s);
        }

        return null;
    }

    public void eliminarServicio(Long id) {
        servicioRepository.deleteById(id);
    }
}
