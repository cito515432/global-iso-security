/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.globalisosecurity.backend.services;

import com.globalisosecurity.backend.models.Empresa;
import com.globalisosecurity.backend.models.Sector;
import com.globalisosecurity.backend.models.Servicio;
import com.globalisosecurity.backend.repositories.EmpresaRepository;
import com.globalisosecurity.backend.repositories.SectorRepository;
import com.globalisosecurity.backend.repositories.ServicioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ServicioService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of(
            "BORRADOR",
            "EN_PROCESO",
            "FINALIZADO",
            "FIRMADO",
            "CERRADO"
    );

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private SectorRepository sectorRepository;

    public List<Servicio> obtenerTodos() {
        return servicioRepository.findAll();
    }

    public Optional<Servicio> obtenerPorId(Long id) {
        return servicioRepository.findById(id);
    }

    public List<Servicio> obtenerPorEstado(String estado) {
        return servicioRepository.findByEstado(normalizarEstado(estado));
    }

    public Servicio crearServicio(Servicio servicio) {
        validarEmpresaYSector(servicio);

        if (servicio.getFechaCreacion() == null) {
            servicio.setFechaCreacion(LocalDateTime.now());
        }

        String estado = servicio.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            estado = "BORRADOR";
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);
        servicio.setEstado(estado);

        Empresa empresa = empresaRepository.findById(servicio.getEmpresa().getId())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Sector sector = sectorRepository.findById(servicio.getSector().getId())
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));

        servicio.setEmpresa(empresa);
        servicio.setSector(sector);

        return servicioRepository.save(servicio);
    }

    public Servicio actualizarServicio(Long id, Servicio servicioActualizado) {
        Servicio servicioExistente = servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (estaBloqueado(servicioExistente.getEstado())) {
            throw new RuntimeException("No se puede modificar un servicio en estado " + servicioExistente.getEstado());
        }

        validarEmpresaYSector(servicioActualizado);

        String estado = servicioActualizado.getEstado();
        if (estado == null || estado.trim().isEmpty()) {
            throw new RuntimeException("El estado del servicio es obligatorio");
        }

        estado = normalizarEstado(estado);
        validarEstado(estado);

        Empresa empresa = empresaRepository.findById(servicioActualizado.getEmpresa().getId())
                .orElseThrow(() -> new RuntimeException("Empresa no encontrada"));

        Sector sector = sectorRepository.findById(servicioActualizado.getSector().getId())
                .orElseThrow(() -> new RuntimeException("Sector no encontrado"));

        servicioExistente.setEmpresa(empresa);
        servicioExistente.setSector(sector);
        servicioExistente.setEstado(estado);

        return servicioRepository.save(servicioExistente);
    }

    public void eliminarServicio(Long id) {
        Servicio servicio = servicioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Servicio no encontrado"));

        if (estaBloqueado(servicio.getEstado())) {
            throw new RuntimeException("No se puede eliminar un servicio en estado " + servicio.getEstado());
        }

        servicioRepository.deleteById(id);
    }

    private void validarEmpresaYSector(Servicio servicio) {
        if (servicio.getEmpresa() == null || servicio.getEmpresa().getId() == null) {
            throw new RuntimeException("La empresa es obligatoria");
        }

        if (servicio.getSector() == null || servicio.getSector().getId() == null) {
            throw new RuntimeException("El sector es obligatorio");
        }
    }

    private void validarEstado(String estado) {
        if (!ESTADOS_VALIDOS.contains(estado)) {
            throw new RuntimeException("Estado no válido. Use: BORRADOR, EN_PROCESO, FINALIZADO, FIRMADO o CERRADO");
        }
    }

    private String normalizarEstado(String estado) {
        return estado.trim().toUpperCase();
    }

    private boolean estaBloqueado(String estado) {
        String estadoNormalizado = normalizarEstado(estado);
        return estadoNormalizado.equals("FIRMADO") || estadoNormalizado.equals("CERRADO");
    }
}